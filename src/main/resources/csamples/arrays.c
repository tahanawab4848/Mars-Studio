/* arrays.c - Array operations for C-to-MIPS demo */
int main() {
    int arr[5];
    int i;
    /* Initialize */
    for (i = 0; i < 5; i++) {
        arr[i] = (i + 1) * 2;
    }
    /* Sum elements */
    int sum = 0;
    for (i = 0; i < 5; i++) {
        sum = sum + arr[i];
    }
    /* Find max */
    int maxVal = arr[0];
    for (i = 1; i < 5; i++) {
        if (arr[i] > maxVal) {
            maxVal = arr[i];
        }
    }
    return sum;
}
