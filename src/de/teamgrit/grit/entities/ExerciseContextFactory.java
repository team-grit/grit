/*
 * Copyright (C) 2014 Team GRIT
 * 
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.teamgrit.grit.entities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

import de.teamgrit.grit.checking.compile.GccCompileChecker;
import de.teamgrit.grit.checking.compile.HaskellCompileChecker;
import de.teamgrit.grit.checking.compile.JavaCompileChecker;
import de.teamgrit.grit.checking.testing.JavaProjectTester;

/**
 * A factory for creating ExerciseContext.
 * 
 * @author <a href="mailto:gabriel.einsdorf@uni-konstanz.de">Gabriel
 *         Einsdorf</a>
 * @author <a href="mailto:stefano.woerner@uni-konstanz.de">Stefano Woerner</a>
 * @author <a href="mailto:david.kolb@uni-konstanz.de">David Kolb</a>
 */

public final class ExerciseContextFactory {

	private static final Logger LOGGER = Logger.getLogger("systemlog");

	private static final Path OUTPUT_BASEPATH = Paths.get(
			System.getProperty("user.dir"), "res", "web", "pdf");

	/**
	 * This class shouldn't get instantiated.
	 */
	private ExerciseContextFactory() {
	}

	/**
	 * Returns an {@link ExerciseContext} for Exercises.
	 * 
	 * @param exerciseId
	 *            the id for the new exercise
	 * @param connectionId
	 *            the connection to the remote where we fetch the exercises
	 *            from.
	 * @param exerciseMetadata
	 *            the metadata of the exercise (name, deadline etc.) report
	 * @param courseId
	 *            the id of the course exercise belongs to
	 * @param courseName
	 *            the name of the course the exercise belongs to
	 * 
	 * @return The exercise context for an exercise
	 * @throws WrongDateException
	 *             if the deadline lies in the past.
	 */
	public static ExerciseContext getExerciseContext(int exerciseId,
			int connectionId, ExerciseMetadata exerciseMetadata, int courseId,
			String courseName) throws WrongDateException {

		/* metadata */
		ExerciseContext context = new ExerciseContext(
				exerciseMetadata.getExerciseName());
		context.setCourseName(courseName);
		context.setStartTime(exerciseMetadata.getStartTime());
		context.setDeadline(exerciseMetadata.getDeadline());
		context.setPeriod(exerciseMetadata.getPeriod());

		Path outputPath = Paths.get("course-" + courseId, "exercise-"
				+ exerciseId);

		Path basePath = Paths.get("wdir", "course-" + courseId, "exercise-"
				+ exerciseId);

		Path testFileLocation = basePath.resolve("tests");

		String fileRegex = null;
		String archiveRegex = null;

		switch (exerciseMetadata.getLanguageType()) {
		case JAVA:
			makeJavaExerciseContext(context,
					exerciseMetadata.getCompilerFlags(), testFileLocation);
			fileRegex = ".+\\.[Jj][Aa][Vv][Aa]";
			archiveRegex = ".+\\.(([Zz][Ii][Pp])|([Jj][Aa][Rr]))";

			break;
		case C:
			makeCExerciseContext(context, exerciseMetadata.getCompilerFlags(),
					testFileLocation);
			fileRegex = ".+\\.[Cc]";
			archiveRegex = ".+\\.[Zz][Ii][Pp]";
			break;

		case CPP:
			makeCppExerciseContext(context,
					exerciseMetadata.getCompilerFlags(), testFileLocation);
			fileRegex = ".+\\.[Cc][Pp][Pp]";
			archiveRegex = ".+\\.[Zz][Ii][Pp]";
			break;

		case HASKELL:
			makeHaskellExerciseContext(context,
					exerciseMetadata.getCompilerFlags(), testFileLocation);
			fileRegex = ".+\\.([Ll])?[Hh][Ss]";
			archiveRegex = ".+\\.[Zz][Ii][Pp]";
			break;

		default:
			throw new IllegalArgumentException(
					"Language Type is not supported!");
		}

		context.setLanguageType(exerciseMetadata.getLanguageType());
		context.setConnectionId(connectionId);

		context.setFileRegex(fileRegex);
		context.setArchiveRegex(archiveRegex);

		context.setTests(testFileLocation);
		context.setBinPath(basePath.resolve("bin"));
		context.setTempPdfPath(basePath.resolve("tempPdf"));
		context.setFetchPath(basePath.resolve("fetch"));
		context.setOutputPath(OUTPUT_BASEPATH.resolve(outputPath));

		return context;

	}

	/**
	 * Transforms an generic {@link ExerciseContext} to one for Haskell
	 * exercises.
	 * 
	 * @param context
	 *            the context
	 * @param compilerFlags
	 *            the compiler flags
	 * @param testFileLocation
	 *            the location of the unit test source code.
	 */
	private static void makeHaskellExerciseContext(ExerciseContext context,
			List<String> compilerFlags, Path testFileLocation) {

		context.setCompiler(new HaskellCompileChecker());
		context.setCompilerName("ghc");
		context.setCompilerFlags(compilerFlags);
		context.setTester(null);

	}

	/**
	 * Transforms an generic {@link ExerciseContext} to one for C exercises.
	 * 
	 * @param context
	 *            the context
	 * @param compilerFlags
	 *            the compiler flags
	 * @param testFileLocation
	 *            the location of the unit test source code.
	 */
	private static void makeCExerciseContext(ExerciseContext context,
			List<String> compilerFlags, Path testFileLocation) {
		context.setCompiler(new GccCompileChecker());
		context.setCompilerName("gcc");

		context.setCompilerFlags(compilerFlags);
		context.setTester(null);
	}

	/**
	 * Transforms an generic {@link ExerciseContext} to one for C++ exercises.
	 * 
	 * @param context
	 *            the context
	 * @param compilerFlags
	 *            the compiler flags
	 * @param testFileLocation
	 *            the location of the unit test source code.
	 */
	private static void makeCppExerciseContext(ExerciseContext context,
			List<String> compilerFlags, Path testFileLocation) {
		context.setCompiler(new GccCompileChecker());
		context.setCompilerName("g++");
		context.setCompilerFlags(compilerFlags);
		context.setTester(null);
	}

	/**
	 * Transforms an generic {@link ExerciseContext} to one for Java exercises.
	 * 
	 * @param context
	 *            the context
	 * @param compilerFlags
	 *            the compiler flags
	 * @param testFileLocation
	 *            the location of the unit test source code.
	 */
	private static void makeJavaExerciseContext(ExerciseContext context,
			List<String> compilerFlags, Path testFileLocation) {
		context.setCompiler(new JavaCompileChecker(testFileLocation));
		context.setCompilerName("javac");
		context.setCompilerFlags(compilerFlags);
		context.setTester(new JavaProjectTester(testFileLocation));
	}
}
