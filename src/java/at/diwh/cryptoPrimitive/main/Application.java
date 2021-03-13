package at.diwh.cryptoPrimitive.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.zip.ZipEntry;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;

import at.diwh.cryptoPrimitive.util.ZippingTools;
import at.diwh.cryptoTools.exception.CryptoException;
import at.diwh.cryptoTools.hid.CryptoHID;
import at.diwh.utils.file.tools.FormatByteAngabe;
import at.diwh.utils.object.Nullchecker;

/**
 * <b>Zweck</b>: Alle Dateien in einem Verzeichnis zu verschlüsseln, um sie so z.B. per Mail
 * übertragen zu können. Ebenfalls natürlich sie wieder zu entschlüsseln. <br/> 
 * <br/>Dieses Programm nimmt alle Dateien aus dem Quellverzeichnis und verarbeitet sie wie folgt:
 * <br/>___ .AES256 Dateien werden entschlüsselt und in das Zielverzeichnis gestellt,
 * <br/>___ .zip oder .ZIP Dateien werden verschlüsselt ins Zielverzeichnis gegeben,
 * <br/>___ alle anderen Dateien werden ins Quellverzeichnis gezippt und dann wie ZIP-Files verarbeitet.
 * <br/> <br/>Eine Besonderheit: Wenn eine Datei gezippt wird, wird ihr Dateiname "randomized". So kann ein
 * Schnüffler auch aus dem Dateinamen nichts ablesen, wenn er die Übertragung (Mail Attachment) beobachtet.
 * Im Inneren der Zip-Datei wird der ursprüngliche Dateiname konserviert.
 * <br/> Bereits im Verzeichnis vorhandene zip-Dateien werden nicht "randomized", nur die, die das Programm selbst in ZIP-Files verwandelt.
 * <br/><b>Hinweis zur Passphrase</b>: Das Programm macht alles in einem Rutsch. D.h. findet es normale Dateien zusammen mit .AES256-Files
 * im Quell-Verzeichnis, wird es die normalen Dateien zippen und verschlüsselt in das Zielverzeichnis pumpen, 
 * die .AES256-Dateien hingegen entschlüsselt in das Zielverzeichnis stellen. Man kann aber nur <b>eine</b> Passphrase
 * eingeben. Daraus folgt zwingend: Will man ver- und entschlüsseln in einem Rutsch, muss die Passphrase der Verschlüsselung
 * die gleiche sein wie die, mit der die .AES256-Dateien erzeugt wurde. Ebenfalls ergibt sich daraus, dass nur AES-Files 
 * zusammen entschlüsselt werden können, die unter sich ebenfalls die gleiche Passphrase haben.
 * @author JavaAlchemist
 *
 */
public class Application {
	
	// ACHTUNG: Ich nutze den FileSeparator, um das Betriebssystem zu unterscheiden (Win/Unix).
	// Im Code verwende ich immer den / weil Unix das sowieso macht und Java damit auch auf Win damit umgehen kann
	public static String FILESEPARATOR = System.getProperty("file.separator");
	public static String HOMEDIR = System.getProperty("user.home");
	
	public static void main(String[] args) throws CryptoException, IOException {
		System.out.println("Willkommen zum einfachsten Verschlüsseln der Welt.");
		// Preparation Block && Basic Check
		String inputDirName = "/Users/devdiwh/Downloads/cryptoInDir";
		String outputDirName ="/Users/devdiwh/Downloads/cryptoOutDir";
		if ("/".equalsIgnoreCase(FILESEPARATOR)) {
			inputDirName = HOMEDIR + "/Downloads/in";
			outputDirName = HOMEDIR + "/Downloads/out";
		}
		if (Nullchecker.istNOL(args) || args.length !=2) {
			System.out.println("Das Programm benötigt genau zwei Parameter: Input-Directory Output-Directory");
			System.out.println("Defaults wurden gesetzt.");
			
		} else {
			inputDirName = args[0];
			if (!new File(inputDirName).isDirectory()) {
				System.out.println("Fehler: " + inputDirName + " ist kein Verzeichnis");
				System.exit(9);
			}
			outputDirName = args[1];
			if (!new File(outputDirName).isDirectory()) {
				System.out.println("Fehler: " + outputDirName + " ist kein Verzeichnis");
				System.exit(10);
			}
		}

		// Prep&Check Block ging gut, also verarbeiten wir.
		
		System.out.println("Werde von \n\t" + inputDirName +"\nlesen und auf \n\t" + outputDirName +"\nschreiben");
		// printKonstanten(); // Ausgabe aller Konstanten, debug
		// hole alle Files vom Quellverzeichnis (keine Sub-Dirs)
		List<String> inputFileNamen = alleFilesAusVerzeichnis(inputDirName);
		
		// DEBUG
//		for (String element : inputFileNamen) {
//			String fqInFileName = inputDirName + "/" + element;
//			String fqOutFileName = outputDirName + "/" + element+".zip";
//			copyAndZipFile(fqInFileName, fqOutFileName, element);
//		}
		
		// DEBUG BREAKER
//		if (1==1) return;
		
		// Bereite die Liste auf; verwandle Datenfiles in ZIP-Archive, nimm .AES256-Dateien mit und bereits vorhandene .zip Files
		List<String> vorbereiteteNamen = zippeAllePlainFiles(inputFileNamen, inputDirName);

		System.out.println("Folgende Dateinamen werden verarbeitet");
		for (String element : vorbereiteteNamen) {
			System.out.println("   " + element);
		}

		// DEBUG BREAKER
//		if (1==1) return;

		// da wir altes Java haben, haben wir kein System.Cosole.readPassword, deswegen der Kunstgriff über SWING...
		System.out.println("Geben Sie nun die Passphrase ein: ");
		String passphrase = null;
		passphrase = passphraseEingabe("Passphrase eingeben");
		// passphrase = benutzerEingabe(); // das war zum Testen, da wird aber die Passphrase am Schirm angezeigt... noGo!
		
		String kontrolle = passphraseEingabe("Passphrase wiederholen!");
		if (!kontrolle.equals(passphrase)) {
			System.out.println("Fehler. Eingaben unterscheiden sich! Abbruch.");
			System.exit(1);
		}
		
		// echte Verarbeitung
		verarbeiteFiles(vorbereiteteNamen, inputDirName, outputDirName, passphrase);
		
	}
	
	/**
	 * Nur für Debug-Zwecke
	 * @throws CryptoException
	 */
	@SuppressWarnings("unused")
	private static void printKonstanten() throws CryptoException {
		System.out.println("Folgende Konstanten sind definiert: ");
		Map<String, String> konstantenMap = CryptoHID.getAllConstants();
		Set<String> keyset = konstantenMap.keySet();
		for (String element : keyset) {
			System.out.println("\tKonstante: " + element + " Wert: " + konstantenMap.get(element));
		}
		System.out.println("\n\tJava Version: " + System.getProperty("java.version"));
	}
	
	/**
	 * War für den Development-Prozess als Beschleunigung. Da man aber das ECHO nicht unterdrücken kann, nutzlos im Betrieb.
	 * @return
	 */
	@SuppressWarnings("unused")
	private static String benutzerEingabe() {
		Scanner sc = new Scanner(System.in);
		String eingabe = sc.nextLine();
		sc.close();
		return eingabe;
	}
	
	/**
	 * Umweg über Swing, weil das ein unsichtbares Passworteingabefeld kennt.
	 * @param meldung
	 * @return
	 */
	private static String passphraseEingabe(String meldung) {
		final JPasswordField pf = new JPasswordField(); 
		String result = JOptionPane.showConfirmDialog(null, pf, meldung,
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE ) == JOptionPane.OK_OPTION ? new String( pf.getPassword() ) : "";
		return result;
	}

	/**
	 * Hier wird das Quellverzeichnis gescannt. 
	 * @param filenamen - Liste der Dateinamen
	 * @param inputDirName - Quelle
	 * @return Liste der zu verarbeitenden Dateinamen
	 * @throws CryptoException
	 * @throws IOException
	 */
	private static List<String> zippeAllePlainFiles(List<String> filenamen, String inputDirName) throws CryptoException, IOException {
		List<String> ergebnis = new ArrayList<String>();
		CryptoHID tmpHID = new CryptoHID();
		for (String element : filenamen) {
			if (!element.endsWith(".AES256") && !(element.endsWith(".zip") || element.endsWith(".ZIP"))) {
				File inputFile = new File(inputDirName + "/" + element);
				String newElementName = randomAbisZString(12);
				File zipOutputFile = new File(inputDirName + "/" + newElementName + ".zip");
				ZipEntry elementZipEntry = new ZipEntry(element);
				byte[] elementData = tmpHID.binaryReadWholeFile(inputFile);
				System.out.println("Zipping... Schreibe " + zipOutputFile.getCanonicalPath());
				ZippingTools.writeZipEntryToNewZipFile(zipOutputFile, elementZipEntry, elementData);
				ergebnis.add(newElementName + ".zip");
			} else {
				ergebnis.add(element);
			}
		}
		return ergebnis;
	}
	
	/**
	 * Wie der Name suggeriert liefert es n Stellen lange, zufällige Strings mit Großbuchstaben.
	 * Das ist nützlich, wenn man garantiert gültige Dateinamen erzeugen will.
	 * @param laenge - Ziellänge des Strings
	 * @return - eine zufällige Folge von Großbuchstaben A-Z
	 */
	private static String randomAbisZString(int laenge) {
		StringBuffer sb = new StringBuffer();
		
		for (int i=0; i<laenge; i++) {
			int randWert = 65 + (int)(Math.random() * ((90 - 65) + 1));
			// int randWert = NumberFunction.liefereZufallszahl(65, 90);
			// System.out.println("Wert: " + randWert);
			sb.append((char) randWert);
		}
		
		return sb.toString();
	}
	

	
	/**
	 * Hier findet die eigentliche Verarbeitung statt. Jedes File wird anhand der File-Extension auf zip oder AEs geprüft, 
	 * und dementsprechend entweder ver- oder entschlüsselt.
	 * @param filenamen - Liste der Filenamen
	 * @param inputDir - Quelle
	 * @param outputDir - Ziel
	 * @param passphrase - für AES
	 * @throws IOException
	 * @throws CryptoException
	 */
	private static void verarbeiteFiles(List<String> filenamen, String inputDir, String outputDir, String passphrase) throws IOException, CryptoException {
		CryptoHID hid = new CryptoHID();
		for (String element : filenamen) {
			System.out.println("Verarbeite " + element);
			File inFile = new File(inputDir + "/" + element);
			byte[] inData = hid.binaryReadWholeFile(inFile);
			byte[] outData = null;
			File outFile = null;
			if (element.endsWith(".AES256")) {
				System.out.println("  Das ist ein AES File -> entschlüssele...");
				String tmpName = outputDir + "/" + element.substring(0, element.lastIndexOf("."));
				outFile = new File(tmpName);
				System.out.println("   ... nach " + outFile.getCanonicalPath());
				outData = hid.aes256_decrypt(passphrase, inData);

			} else if (element.endsWith(".zip") || element.endsWith(".ZIP")){
				System.out.println("  Das ist ein ZIP File -> verschlüssele...");
				outFile = new File(outputDir + "/" + element + ".AES256");
				System.out.println("   ... nach " + outFile.getCanonicalPath());
				outData = hid.aes256_encrypt(passphrase, inData);

			} else {
				System.out.println("Weder ZIP noch AES File. Skipping " + element);
				continue;
			}
			System.out.println("-> schreibe " + outFile.getCanonicalPath());
			hid.binaryWriteWholeFile(outFile, outData);
			System.out.println();
		}
	}
	
	/**
	 * Das ist eine Debugging-Methode gewesen. Hat einen Fehler offenbart, der in den SVB Utils war (von mir ein Fehler!)
	 * @param quelle
	 * @param ziel
	 * @param dateiName
	 * @throws CryptoException
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private static void copyAndZipFile(String quelle, String ziel, String dateiName) throws CryptoException, IOException {
		System.out.println("DEBUG METHODE");
		System.out.println("Copying " + quelle + " nach " + ziel);
		File quellFile = new File(quelle);
		File zielFile = new File(ziel);
		CryptoHID hid = new CryptoHID();
		byte[] inputData = hid.binaryReadWholeFile(quellFile);

		// COPY
		// hid.binaryWriteWholeFile(zielFile, inputData);
		
		// ZIP
		// System.out.println("Ich zippe: " + dateiName);
		ZipEntry ze = new ZipEntry(dateiName);
		ZippingTools.writeZipEntryToNewZipFile(zielFile, ze, inputData);
		
		String testPassphrase = "ABCDEF";
		String encFileName = ziel+".AES256";
		File encFile = new File(encFileName);
		System.out.println("Lese: " + zielFile.getCanonicalPath());
		byte[] inputZippedData = hid.binaryReadWholeFile(zielFile);
		System.out.println("Datei gelesen, Größe: " + FormatByteAngabe.lesbareDateigroesse(inputZippedData.length));
		byte[] encData = hid.aes256_encrypt(testPassphrase, inputZippedData);
		System.out.println("Schreibe " + FormatByteAngabe.lesbareDateigroesse(encData.length) 
				+ "in File " + encFile.getCanonicalPath());
		hid.binaryWriteWholeFile(encFile, encData);
		
		
	}
	
	/**
	 * Holt alle Dateien (keine Verzeichnisse!) aus dem Verzeichnis, das übergeben wurde. Geht nicht in Unterverzeichnisse hinein!
	 * @param verzeichnis
	 * @return Liste von Dateinamen
	 */
	private static List<String> alleFilesAusVerzeichnis(String verzeichnis) {
		List<String> results = new ArrayList<String>();

		File[] files = new File(verzeichnis).listFiles(); // wäre null wenn kein Verzeichnis, kann aber nicht passieren
		System.out.println("Scanne ...");
		for (File file : files) {
		    if (file.isFile()) {
		    	System.out.println("  " + file.getName());
		        results.add(file.getName());
		    }
		}
		System.out.println("... done.");

		return results;
	}

}
