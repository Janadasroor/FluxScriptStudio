// Functions and Recursion Test
fun factorial(n) {
    if (n <= 1) return 1;
    return n * factorial(n - 1);
}

var res = factorial(5);
print("Factorial of 5 is: " + res);
// Expected: Factorial of 5 is: 120
