// FluxScript 0.1.0 Smoke Test
fun calculate(a, b) {
    var result = (a + b) * 2;
    if (result > 10) {
        return "High: " + result;
    }
    return "Low: " + result;
}

print(calculate(5, 3));
