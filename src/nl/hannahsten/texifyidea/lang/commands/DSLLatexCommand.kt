package nl.hannahsten.texifyidea.lang.commands

import nl.hannahsten.texifyidea.lang.LatexPackage


/**
 * We have
 * ```
 *
 *
 * ```
 *
 */
data class LatexCommandImpl(
    override val identifier: String,
    override val command: String,
    override val dependency: LatexPackage,
    override val description: String,
    override val isMathMode: Boolean,
    override val arguments: Array<out Argument>,
    override val display: String?,
) : LatexCommand{
    override val commandWithSlash: String = "\\$command"
}




class LatexCommandBuilderScope {
    private var dependency: LatexPackage = LatexPackage.DEFAULT

    var mathMode: Boolean = false

    private val commands = mutableListOf<LatexCommand>()

    /**
     *
     * `package` is a reserved keyword in Kotlin, so we use `packageOf` instead.
     */
    fun packageOf(name: String) {
        dependency = LatexPackage(name)
    }

    operator fun String.invoke(
        vararg arguments: Argument,
        display: String? = null,
        description: String = "",
    ): LatexCommand {
        val commandText = this
        val identifier = dependency.name + "." + this
        val command = LatexCommandImpl(
            identifier,
            command = commandText,
            dependency = dependency,
            description = description,
            isMathMode = mathMode,
            arguments = arguments,
            display = display
        )
        commands.add(command)
        return command
    }


    companion object{

        fun buildCommands(action : LatexCommandBuilderScope.() -> Unit): List<LatexCommand> {
            val scope = LatexCommandBuilderScope()
            scope.action()
            return emptyList() // This should return the list of commands built in the scope
        }
    }

}
