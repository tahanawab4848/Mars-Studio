/* test_all_tools.c
 * This C code is compiled by MARS's built-in transpiler.
 * We have patched the transpiler to support array assignment!
 */

int arr[10];
int result;
int fact_result;

/* -------------------------------------------------- */
/*  Array Initialization & Bubble Sort                */
/*  Exercises: Array Visualizer                       */
/* -------------------------------------------------- */
int init_array() {
    arr[0] = 42;
    arr[1] = 7;
    arr[2] = 99;
    arr[3] = 3;
    arr[4] = 55;
    arr[5] = 18;
    arr[6] = 71;
    arr[7] = 26;
    arr[8] = 84;
    arr[9] = 11;
    return 0;
}

int bubble_sort(int n) {
    int i;
    int j;
    int temp;
    i = 0;
    while (i < n - 1) {
        j = 0;
        while (j < n - i - 1) {
            if (arr[j] > arr[j + 1]) {
                /* Swap */
                temp = arr[j];
                arr[j] = arr[j + 1];
                arr[j + 1] = temp;
            }
            j = j + 1;
        }
        i = i + 1;
    }
    return 0;
}

/* -------------------------------------------------- */
/*  Recursive factorial                               */
/*  Exercises: Stack Visualizer                       */
/* -------------------------------------------------- */
int factorial(int n) {
    if (n <= 1) {
        return 1;
    }
    return n * factorial(n - 1);
}

/* -------------------------------------------------- */
/*  Main                                              */
/* -------------------------------------------------- */
int main() {
    /* 1. Array Visualizer test */
    init_array();
    bubble_sort(10);

    /* 2. Stack Visualizer test */
    fact_result = factorial(5);

    return 0;
}
