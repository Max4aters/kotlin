// WITH_RUNTIME

/*
 * KOTLIN CODEGEN BOX SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-222
 * PLACE: statements, assignments, simple-assignments -> paragraph 2 -> sentence 1
 * NUMBER: 2
 * DESCRIPTION: If a property has a setter, it is called using the right-hand side expression as its argument;
 */


var flag1 = false
var flag2 = false
val valToSet = 5


var counter = 0
    set(value) {
        flag1 = true
        if (value == valToSet)
            flag2 = true
        field = value
    }


fun box(): String {
    assert(!flag1)
    assert(!flag2)
    counter = valToSet
    if (flag1 && flag2 && counter == valToSet) return "OK"
    return "NOK"
}
