import java.io.File;

import javax.swing.filechooser.FileFilter;

public class XmlFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		if (f.isDirectory()) {
			return true;
		}

		String extension = getExtension(f);
		if (extension != null) {
			extension = extension.toLowerCase();
			if (extension.equals("xml")) {
				return true;
			}
			else {
				return false;
			}
		}
		return false;
	}

	@Override
	public String getDescription() {
		return "XML Files";
	}

	/*
	 * Get the extension of a file.
	 */
	public static String getExtension(File f) {
		try {
			String ext = null;
			String s = f.getName();
			int i = s.lastIndexOf('.');

			if (i > 0 && i < s.length() - 1) {
				ext = s.substring(i + 1).toLowerCase();
			}
			return ext;
		}
		catch (Exception e) {
			System.out.println(e.toString());
			return " ";
		}
	}
}
