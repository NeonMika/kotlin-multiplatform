import kotlinx.coroutines.runBlocking

fun mainX() {
    runBlocking {
        val downloader = MaterialDesignDownloader()
        downloader.printAll(StringBuilder())
    }
}