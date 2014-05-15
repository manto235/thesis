package crawler;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;

import static java.nio.file.FileVisitResult.*;


public class CounterAndDeleterFileVisitor extends SimpleFileVisitor<Path> {

	private final PathMatcher matcher;
	private int numMatches = 0;

	CounterAndDeleterFileVisitor(String pattern) {
		matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
	}

	// Compares the glob pattern against the file or directory name.
	void find(Path file) {
		Path name = file.getFileName();
		if (name != null && matcher.matches(name)) {
			numMatches++;
			try {
				Files.delete(file);
			} catch (IOException e) {
				System.out.println("Cannot delete cookie file: " + file.getFileName());
			}
		}
	}

	// Prints the total number of matches to standard out.
	int done() {
		return numMatches;
	}

	// Invoke the pattern matching method on each file.
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
		find(file);
		return CONTINUE;
	}

	// Invoke the pattern matching method on each directory.
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
		find(dir);
		return CONTINUE;
	}

	/*@Override
	public FileVisitResult postVisitDirectory(Path dir,  IOException exc) throws IOException {
		Files.delete(dir);
		return CONTINUE;
	}*/

	/*@Override
    public FileVisitResult postVisitDirectory(Path directory, IOException ioe)
            throws IOException {
        System.out.println("Deleting Directory: " + directory.getFileName());
        Files.delete(directory);
        return FileVisitResult.CONTINUE;
    }*/

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) {
		System.out.println("Error in CounterAndDeleterFileVisitor");
		System.err.println(exc);
		return CONTINUE;
	}
}
