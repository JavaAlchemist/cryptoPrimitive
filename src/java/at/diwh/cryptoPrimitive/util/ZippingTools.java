package at.diwh.cryptoPrimitive.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import at.diwh.utils.date.Datumsformatierer;
import at.diwh.utils.enums.ContentTypes;
import at.diwh.utils.file.tools.FormatByteAngabe;
import at.diwh.utils.object.Nullchecker;

public class ZippingTools {

	/**
	 * Soll eine Liste aller Einträge liefern, die das Zip-File hat. Weil ein .zip-File ist de facto ein Zipfile.
	 * @param f - das .zip-File
	 * @return Liste von ZipEntries
	 * @throws IOException
	 * @author 246J
	 */
	public static List<ZipEntry> fetchZipDirectory(File f) throws IOException {
		List<ZipEntry> retList = new ArrayList<ZipEntry>();
		FileInputStream fi = new FileInputStream(f);
		ZipInputStream zin = new ZipInputStream(fi);
		retList = fetchZipDirectory(zin);
		fi.close();
		return retList;
	}
	
	/**
	 * Soll eine Liste aller Einträge liefern, die ein byte-Array hat, wenn dieses Array faktisch ein Zipfile ist.
	 * @param b - das byte-Array, das faktisch ein .zip-File enthält
	 * @return Liste von ZipEntries
	 * @throws IOException
	 * @author 246J
	 */
	public static List<ZipEntry> fetchZipDirectory(byte[] b) throws IOException {
		List<ZipEntry> retList = new ArrayList<ZipEntry>();
		ByteArrayInputStream fi = new ByteArrayInputStream(b);
		ZipInputStream zin = new ZipInputStream(fi);
		retList = fetchZipDirectory(zin);
		fi.close();
		return retList;
	}
	
	/**
	 * Soll eine Liste aller Einträge liefern, die beliebiger <i>InputStream</i> hat, wenn dieser Stream faktisch ein Zipfile ist.
	 * @param s - der InputStream, das faktisch ein .zip-File enthält
	 * @return Liste von ZipEntries
	 * @throws IOException
	 * @author 246J
	 */
	public static List<ZipEntry> fetchZipDirectory(InputStream s) throws IOException {
		List<ZipEntry> retList = new ArrayList<ZipEntry>();
		ByteArrayInputStream fi = (ByteArrayInputStream) s;
		ZipInputStream zin = new ZipInputStream(fi);
		retList = fetchZipDirectory(zin);
		fi.close();
		return retList;
	}
	
	/**
	 * Das hier ist die zentrale Methode, ein Zip-Direcrory auszulesen.
	 * Egal woher es kommt, File, byte-Array, InputStream, Brieftaube... es wird hier gelesen.
	 * @param zin - ZipInputStream
	 * @return das Directory als Liste von ZipEntries
	 * @throws IOException
	 * @author 246J
	 */
	public static List<ZipEntry> fetchZipDirectory(ZipInputStream zin) throws IOException { 
		List<ZipEntry> retList = new ArrayList<ZipEntry>();
		ZipEntry z = zin.getNextEntry();
		while (z != null) {
			retList.add(new ZipEntry(z));
			z = zin.getNextEntry();
		}
		zin.close();
		return retList;
	}
	
	/**
	 * Liest aus einem übergebenen File einen bestimmten Eintrag aus. Der Eintrag muss als ZipEntry-Objekt übergeben werden.
	 * @param f - das File, von dem gelesen werden soll
	 * @param ze - der ZipEntry, dessen Datengelesen werden sollen
	 * @return die Daten als byte-Array
	 * @throws IOException
	 * @author 246J
	 */
	public static byte[] loadDataFromZipEntry(File f, ZipEntry ze) throws IOException {
		FileInputStream fi = new FileInputStream(f);
		ZipInputStream zin = new ZipInputStream(fi);
		byte[] returnBarr = loadDataFromZipEntry(zin, ze);
		fi.close();
		return returnBarr;
	}
	
	/**
	 * Liest aus einem übergebenen Byte-Array einen bestimmten Eintrag aus. Der Eintrag muss als ZipEntry-Objekt übergeben werden.
	 * @param b - das Byte-Array, von dem gelesen werden soll
	 * @param ze - der ZipEntry, dessen Datengelesen werden sollen
	 * @return die Daten als byte-Array
	 * @throws IOException
	 * @author 246J
	 */
	public static byte[] loadDataFromZipEntry(byte[] b, ZipEntry ze) throws IOException {
		ByteArrayInputStream fi = new ByteArrayInputStream(b);
		ZipInputStream zin = new ZipInputStream(fi);
		byte[] returnBarr = loadDataFromZipEntry(zin, ze);
		fi.close();
		return returnBarr;
	}
	
	/**
	 * Liest aus einem beliebigen <i>InputStream</i> einen bestimmten Eintrag aus. Der Eintrag muss als ZipEntry-Objekt übergeben werden.
	 * @param s - der InputStream
	 * @param ze - der ZipEntry, dessen Datengelesen werden sollen
	 * @return die Daten als byte-Array
	 * @throws IOException
	 * @author 246J
	 */
	public static byte[] loadDataFromZipEntry(InputStream s, ZipEntry ze) throws IOException {
		ByteArrayInputStream fi = (ByteArrayInputStream) s;
		ZipInputStream zin = new ZipInputStream(fi);
		byte[] returnBarr = loadDataFromZipEntry(zin, ze);
		fi.close();
		return returnBarr;
	}
	
	/**
	 * Das ist die zentrale Lesemethode. Wird über die verschiedenen public-Methoden mit den
	 * anwendungstauglichen Parameterlisten gerufen. Aber egal was daher kommt, <b>gelesen</b> wird hier.
	 * @param zin - ein ZipInputStream
	 * @param ze - das Zip-Entry, dessen Daten gelesen werden sollen
	 * @return die Daten als byte-Array
	 * @throws IOException
	 * @author 246J
	 */
	public static byte[] loadDataFromZipEntry(ZipInputStream zin, ZipEntry ze) throws IOException {
		byte[] buffer = new byte[1024];
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ZipEntry z = zin.getNextEntry();
		
		suchBreakMarker: // Marker für Break
		while (z != null) {
			if (areTheyEqual(ze, z, true)) {
				int len;
				while ((len = zin.read(buffer)) > 0) {
					output.write(buffer, 0, len);
				}
				break suchBreakMarker; // verlasse while-Schleife
			}
			z = zin.getNextEntry();
		}
		
		zin.close();
		return output.toByteArray();
		
	}
	
	/**
	 * Methode schreibt eine ganze Sammlung von ZipEntries samt Daten in ein File. Die ZipEntries sind in einer Map als Keys
	 * gespeichert und als Wert zum Key ist das Byte-Array (die Daten) in der Map. 
	 * <br/>Die Reihenfolge
	 * @param f - das Zipfile, das geschrieben werden soll
	 * @param dataMap Key: ZipEntry, Value: byte[]
	 * @throws IOException
	 * @author 246J
	 */
	public static void writeZipEntryToNewZipFile(File f, Map<ZipEntry, byte[]> dataMap) throws IOException {
		FileOutputStream fo = new FileOutputStream(f);
		ZipOutputStream zos = new ZipOutputStream(fo);
		
		Set<Entry<ZipEntry, byte[]>> mapEntries = dataMap.entrySet();
		
		for (Entry<ZipEntry, byte[]> e : mapEntries) {
			zos.putNextEntry(new ZipEntry(e.getKey().getName()));
			zos.write(e.getValue());
		}

		zos.close();
		fo.close();
	}
	
	/**
	 * Schreibe ein einziges ZipEntry in das File. Diese Methode ist rein zur Bequemlichkeit, damit der
	 * Anwender nicht für ein einziges Element eine Map aufspannen und befüllen muss.
	 * @param f - das Zipfile, das geschrieben werden soll
	 * @param z - ZipEntry
	 * @param data - binäre Daten des zu zippenden Files (byte[])
	 * @return das Zipfile als Byte-Array
	 * @throws IOException
	 * @author 246J
	 */
	public static void writeZipEntryToNewZipFile(File f, ZipEntry z, byte[] data) throws IOException {
		Map<ZipEntry, byte[]> schreibMap = new HashMap<ZipEntry, byte[]>();
		schreibMap.put(z, data); // wir tun nur ein Element rein
		writeZipEntryToNewZipFile(f, schreibMap); // und schreiben
	}
	
	/**
	 * Methode schreibt eine ganze Sammlung von ZipEntries samt Daten in ein Byte-Array als ob es ein File wäre. 
	 * Die ZipEntries sind in einer Map als Keys gespeichert und als Wert zum Key ist das Byte-Array (die Daten) in der Map. 
	 * @param dataMap Key: ZipEntry, Value: byte[]
	 * @throws IOException
	 * @returns ein byte[], das das gesamte Zipfile enthält
	 * @author 246J
	 */
	public static byte[] writeZipEntryToNewZipFile(Map<ZipEntry, byte[]> dataMap) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(bos);
		
		Set<Entry<ZipEntry, byte[]>> mapEntries = dataMap.entrySet();
		for (Entry<ZipEntry, byte[]> e : mapEntries) {
			zos.putNextEntry(new ZipEntry(e.getKey().getName()));
			zos.write(e.getValue());
		}

		zos.close();
		bos.close();
		return bos.toByteArray();
	}
	
	/**
	 * Schreibe ein einziges ZipEntry in das Pseudo-File (Byte-Array). Diese Methode ist rein zur Bequemlichkeit, damit der
	 * Anwender nicht für ein einziges Element eine Map aufspannen und befüllen muss.
	 * @param z - ZipEntry
	 * @param data - binäre Daten des zu zippenden Files (byte[])
	 * @return das Zipfile als Byte-Array
	 * @throws IOException
	 * @author 246J
	 */
	public static byte[] writeZipEntryToNewZipFile(ZipEntry z, byte[] data) throws IOException {
		Map<ZipEntry, byte[]> schreibMap = new HashMap<ZipEntry, byte[]>();
		schreibMap.put(z, data); // wir tun nur ein Element rein
		return writeZipEntryToNewZipFile(schreibMap);
	}
	
	/**
	 * Gibt die Metadaten (ZipEntry) eines komprimierten Inhalts aus.
	 * @param ze
	 * @return einen formatierten String mit den Attributwerten
	 * @author 246J
	 */
	public static String toStringZipEntry(ZipEntry ze) {
		StringBuffer sb = new StringBuffer();
		sb.append("\nDatei-Name          : " + ze.getName());
		sb.append("\nDatei-Kommentar     : " + ze.getComment());
		sb.append("\nDatei-Größe         : " + FormatByteAngabe.lesbareDateigroesse((int)ze.getSize()));
		sb.append("\nDatei-Größe (kompr.): " + FormatByteAngabe.lesbareDateigroesse((int)ze.getCompressedSize()));
		sb.append("\nDatei-Zeitstempel   : " + Datumsformatierer.ultraflexiFormatDatum(new Date(ze.getTime()),32));
		return sb.toString();
	}
	
	/**
	 * Vergleicht zwei ZipEntry-Objekte anhand ihres Namens. Gibt <i>true</i> zurück, wenn die Attribute, die Gleichheit definieren, gleich sind. 
	 * <br/>Kann streng prüfen (strict=true) oder die Groß/Klein-Schreibung ignorieren (strict=false)
	 * @param a
	 * @param b
	 * @param strict - setze auf true, wenn Groß-Klein-Schreibung beachtet werden soll
	 * @return true, wenn die Entries nach den relevanten Aspekten "gleich" sind
	 * @author 246J
	 */
	public static boolean areTheyEqual(ZipEntry a, ZipEntry b, boolean strict) {
		if (strict) {
			return a.getName().equals(b.getName());
		}
		return a.getName().equalsIgnoreCase(b.getName());
	}
	
	/**
	 * Holt sich aus der Dateiendung den passenden ContentType. Endungen werden immer in Kleinbuchstaben verarbeitet. .tXt ist also gleich .txt, etc.
	 * <br/> Retourniert ContentTypes.CONTENT_ALLG wenn kein spezifisch passender Typ gefunden wurde.
	 * @param ze Ein ZipEntry
	 * @return  at.sozvers.svb.utils.enums.ContentTypes oder ContentTypes.CONTENT_ALLG
	 * @author 246J
	 */
	public static ContentTypes fetchMimeType(ZipEntry ze) {
		String endung = null;
		try {
			endung = ze.getName().substring(ze.getName().lastIndexOf('.')+1).toLowerCase();
		} catch (Exception e) { // da ist wohl keine Endung zustande gekommen
			return ContentTypes.CONTENT_ALLG;
		}
		if (Nullchecker.istNOL(endung)) {
			return ContentTypes.CONTENT_ALLG;
		}
		for (ContentTypes ct : ContentTypes.values()) {
			List<String> vergleichsListe = Arrays.asList(ct.getEndungen());
			if (vergleichsListe.contains(endung)) {
				return ct;
			}
		}
		return ContentTypes.CONTENT_ALLG;
	}
	
	
}
