class StringSorting {
    companion object {
        fun sort(data : Collection<String>, comp : Comparator<String>): List<String> {
            return data.sortedWith(comp)
        }
    }
}