/* loops.c - Loop constructs for C-to-MIPS demo */
int main() {
    int sum = 0;
    /* for loop: sum 1..10 */
    for (int i = 1; i <= 10; i++) {
        sum = sum + i;
    }
    /* while loop: countdown */
    int n = 5;
    int prod = 1;
    while (n > 0) {
        prod = prod * n;
        n = n - 1;
    }
    return sum + prod;
}
