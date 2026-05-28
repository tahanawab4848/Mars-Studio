/* branches.c - if/else branching for C-to-MIPS demo */
int max(int a, int b) {
    if (a > b) {
        return a;
    } else {
        return b;
    }
}

int classify(int n) {
    if (n < 0)       return -1;
    else if (n == 0) return 0;
    else             return 1;
}

int main() {
    int x = max(7, 3);
    int y = classify(-5);
    int z = classify(0);
    return x + y + z;
}
