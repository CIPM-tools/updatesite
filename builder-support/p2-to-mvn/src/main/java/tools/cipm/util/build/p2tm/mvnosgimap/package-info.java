/**
 * Contains the means to parse local Maven repositories to create an index of
 * its contents, in the form of {@link MvnRepositoryIndex}. Parses the index
 * based on the JAR files in the local Maven repository instead of the provided
 * items.json file, because it does not contain any information on the exported
 * packages of the individual Maven dependencies. Represents the Maven
 * coordinates of the individual JAR files via {@link Coordinate}.
 * <p>
 * In the current setup, the local Maven repository is located at the top level
 * directory of the entire GIT repository, under the "mvn" folder.
 */
package tools.cipm.util.build.p2tm.mvnosgimap;