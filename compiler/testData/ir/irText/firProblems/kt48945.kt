// JVM_TARGET: 1.8
// FULL_JDK

class Test : Map<String, String>, java.util.AbstractMap<String, String>() {
    override val entries: MutableSet<MutableMap.MutableEntry<String, String>>
        get() = throw Exception()
}

fun box(): String {
    Test().remove(null, "")
    return "OK"
}
