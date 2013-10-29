package org.lysty.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

public class Updater {

	/**
	 * @param args
	 *            0: patch zip file location 1: the folder to update using the
	 *            patch 2: temp folder to use for unzipping
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ZipException
	 */
	public static void main(String[] args) throws FileNotFoundException,
			IOException, ZipException {

		if ("create".equalsIgnoreCase(args[0])) {
			createPatch(new File(args[1]), new File(args[2]),
					new File(args[3]), new File(args[4]));
		} else if ("apply".equalsIgnoreCase(args[0])) {
			applyPatch(new File(args[1]), new File(args[2]), new File(args[3]));
		} else {
			System.out
					.println("Usage: create oldDir newDir tempDir zipFile | apply zipFile toDir tempDir");
		}
	}

	public static void applyPatch(File fromZip, File toDir, File tempDir)
			throws ZipException, FileNotFoundException, IOException {
		ZipFile zip = new ZipFile(fromZip.getAbsolutePath());
		zip.extractAll(tempDir.getAbsolutePath());
		File to = toDir;
		File from = tempDir;
		copyReplace(from, to);
		File deletesFile = new File(tempDir.getAbsolutePath() + File.separator
				+ "deletes.txt");
		Scanner scan = new Scanner(deletesFile);
		String deleteFilePath;
		while (scan.hasNextLine()) {
			deleteFilePath = scan.nextLine().trim();
			Files.deleteIfExists(Paths.get(deleteFilePath));
			System.out.println("Deleted file  " + deleteFilePath);
		}
		deleteFolder(tempDir);
	}

	private static void deleteFolder(File dir) throws IOException {
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				deleteFolder(file);
			}
			Files.deleteIfExists(Paths.get(file.getAbsolutePath()));
		}
		Files.deleteIfExists(Paths.get(dir.getAbsolutePath()));
	}

	public static void createPatch(File oldDir, File newDir, File tempDiffDir,
			File zipFile) throws IOException, ZipException {

		createDiff(oldDir, newDir, tempDiffDir);
		List<String> deletes = getDeleteFiles(oldDir, newDir);
		int oldDirPathLen = oldDir.getAbsolutePath().length();

		FileWriter writer = new FileWriter(new File(tempDiffDir
				+ File.separator + "deletes.txt"));
		for (String delete : deletes) {
			delete = delete.substring(oldDirPathLen + 1);
			writer.write(delete + System.getProperty("line.separator"));
			System.out.println("Adding delete entry " + delete);
		}
		writer.close();
		ZipFile zip = new ZipFile(zipFile);
		ZipParameters param = new ZipParameters();
		param.setIncludeRootFolder(false);
		zip.createZipFileFromFolder(tempDiffDir, param, false, 0);
		deleteFolder(tempDiffDir);

	}

	private static void createZip(ZipFile zip, File dir, ZipParameters param)
			throws ZipException {
		File[] files = dir.listFiles();
		for (File file : files) {

			zip.addFile(file, param);
			if (file.isDirectory()) {
				createZip(zip, file, param);
			}
		}
	}

	private static List<String> getDeleteFiles(File oldDir, File newDir) {
		List<String> list = new ArrayList<String>();
		File[] files = oldDir.listFiles();
		File newDirFile;
		for (File file : files) {
			newDirFile = new File(newDir.getAbsolutePath() + File.separator
					+ file.getName());
			if (!newDirFile.exists()) {
				list.add(file.getAbsolutePath());
			} else {
				if (newDirFile.isDirectory()) {
					list.addAll(getDeleteFiles(file, newDirFile));
				}
			}
		}
		return list;
	}

	public static void createDiff(File oldDir, File newDir, File diffDir)
			throws IOException {
		if (!diffDir.exists()) {
			diffDir.mkdir();
		}
		File[] files = newDir.listFiles();
		File oldFile;
		File diffFile;
		for (File file : files) {
			oldFile = new File(oldDir.getAbsolutePath() + File.separator
					+ file.getName());
			diffFile = new File(diffDir.getAbsolutePath() + File.separator
					+ file.getName());
			if (file.isDirectory()) {
				if (!oldFile.exists()) {
					copyDirectory(file, diffFile);
				} else {
					createDiff(oldFile, file, diffFile);
				}
			} else {
				if (!oldFile.exists()
						|| oldFile.lastModified() < file.lastModified()) {
					diffFile = new File(diffDir.getAbsolutePath()
							+ File.separator + file.getName());
					Files.copy(Paths.get(file.getAbsolutePath()),
							Paths.get(diffFile.getAbsolutePath()),
							StandardCopyOption.REPLACE_EXISTING);
					System.out.println("Adding " + diffFile);
				}
			}
		}
	}

	private static void copyDirectory(File from, File to) throws IOException {
		if (!to.exists()) {
			to.mkdir();
		}
		File[] files = from.listFiles();
		File toFile;
		for (File file : files) {
			toFile = new File(to.getAbsolutePath() + File.separator
					+ file.getName());
			if (file.isDirectory()) {
				copyDirectory(file, toFile);
			} else {
				Files.copy(Paths.get(file.getAbsolutePath()),
						Paths.get(toFile.getAbsolutePath()));
				System.out.println("Copy file " + file.getAbsolutePath()
						+ " to " + toFile.getAbsolutePath());

			}
		}
	}

	public static void copyReplace(File fromDir, File toDir)
			throws FileNotFoundException, IOException {
		File[] files = fromDir.listFiles();
		File toFile;
		for (File file : files) {
			toFile = new File(toDir.getAbsolutePath() + File.separator
					+ file.getName());
			if (file.isDirectory()) {
				toFile = new File(toDir.getAbsolutePath() + File.separator
						+ file.getName());
				if (!toFile.exists()) {
					toFile.mkdir();
				}
				copyReplace(file, toFile);
			} else {
				Files.copy(new FileInputStream(file),
						Paths.get(toFile.getAbsolutePath()),
						StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Replacing " + toFile.getAbsolutePath());
			}
		}
	}
}
