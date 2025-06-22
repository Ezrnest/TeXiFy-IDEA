package nl.hannahsten.texifyidea.completion.pathcompletion

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import nl.hannahsten.texifyidea.index.NewIncludesIndex
import nl.hannahsten.texifyidea.util.files.findRelativeSearchPathsForImportCommands

/**
 * Provide base folder for autocompleting folders.
 */
class LatexFolderProvider : LatexPathProviderBase() {

    override fun selectScanRoots(file: PsiFile): List<VirtualFile> {
        val searchDirs = getProjectRoots().toMutableList()
        NewIncludesIndex.traverseAll(file) { command ->
            searchDirs.addAll(findRelativeSearchPathsForImportCommands(command))
            true
        }
        return searchDirs
    }

    override fun searchFolders(): Boolean = true

    override fun searchFiles(): Boolean = false
}