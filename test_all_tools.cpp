/*
 * test_all_tools.cpp
 *
 * C++ equivalent of test_all_tools.asm.
 * Implements the same four test sections so you can verify expected
 * outputs before running the MIPS version inside MARS:
 *
 *   1. Arithmetic Showcase  – exercises various integer ALU ops
 *   2. Bubble Sort          – sorts a 10-element array (feeds Array Visualizer)
 *   3. Recursive Factorial  – deep call stack  (feeds Stack Visualizer)
 *   4. Iterative Fibonacci  – tight register loop (feeds Pipeline Visualizer)
 *
 * Compile:   g++ -std=c++17 -o test_all_tools test_all_tools.cpp
 * Run:        ./test_all_tools
 */

#include <iostream>
#include <iomanip>
#include <vector>
#include <cstdint>

// ================================================================
//  1. ARITHMETIC SHOWCASE
// ================================================================

void test_arithmetic() {
    std::cout << "\n=== Arithmetic Showcase ===\n";

    int t0 = 100, t1 = 37;

    int add_r  = t0 + t1;                 // ADD
    int sub_r  = t0 - t1;                 // SUB
    int mul_r  = t0 * t1;                 // MUL
    int and_r  = t0 & t1;                 // AND
    int or_r   = t0 | t1;                 // OR
    int xor_r  = t0 ^ t1;                 // XOR
    int nor_r  = ~(t0 | t1);             // NOR
    int slt_r  = (t1 < t0) ? 1 : 0;      // SLT
    int sll_r  = t0 << 2;                 // SLL  (×4)
    int srl_r  = (unsigned)t0 >> 1;       // SRL  (÷2 logical)
    int sra_r  = t1 >> 1;                 // SRA  (÷2 arithmetic)
    int addi_r = t0 + (-5);               // ADDI
    int andi_r = t0 & 0xFF;               // ANDI
    int ori_r  = t1 | 0x80;              // ORI

    std::cout << std::left;
    std::cout << std::setw(20) << "ADD  100+37"  << " = " << add_r  << "\n";
    std::cout << std::setw(20) << "SUB  100-37"  << " = " << sub_r  << "\n";
    std::cout << std::setw(20) << "MUL  100*37"  << " = " << mul_r  << "\n";
    std::cout << std::setw(20) << "AND  100&37"  << " = " << and_r  << "\n";
    std::cout << std::setw(20) << "OR   100|37"  << " = " << or_r   << "\n";
    std::cout << std::setw(20) << "XOR  100^37"  << " = " << xor_r  << "\n";
    std::cout << std::setw(20) << "NOR  ~(100|37)"<< " = " << nor_r  << "\n";
    std::cout << std::setw(20) << "SLT  37<100"  << " = " << slt_r  << "\n";
    std::cout << std::setw(20) << "SLL  100<<2"  << " = " << sll_r  << "\n";
    std::cout << std::setw(20) << "SRL  100>>1"  << " = " << srl_r  << "\n";
    std::cout << std::setw(20) << "SRA  37>>1"   << " = " << sra_r  << "\n";
    std::cout << std::setw(20) << "ADDI 100+(-5)"<< " = " << addi_r << "\n";
    std::cout << std::setw(20) << "ANDI 100&0xFF"<< " = " << andi_r << "\n";
    std::cout << std::setw(20) << "ORI  37|0x80" << " = " << ori_r  << "\n";

    // Small accumulator loop (mirrors arith_loop in MIPS)
    int acc = 0, step = 8;
    std::cout << "\nLoop (acc += 3, mul step): ";
    while (acc < 30) {
        acc  += 3;
        int prod = acc * step;
        int diff = prod - acc;
        std::cout << diff << " ";
    }
    std::cout << "\n";
}

// ================================================================
//  2. BUBBLE SORT
// ================================================================

void print_array(const std::vector<int>& arr, const std::string& label) {
    std::cout << label;
    for (int v : arr) std::cout << v << " ";
    std::cout << "\n";
}

void test_sort() {
    std::cout << "\n=== Bubble Sort ===\n";

    std::vector<int> arr = {42, 7, 99, 3, 55, 18, 71, 26, 84, 11};
    print_array(arr, "Before: ");

    int n = static_cast<int>(arr.size());
    for (int i = 0; i < n - 1; i++) {
        for (int j = 0; j < n - 1 - i; j++) {
            if (arr[j] > arr[j + 1]) {
                std::swap(arr[j], arr[j + 1]);   // mirrors sw/lw swap in MIPS
            }
        }
    }

    print_array(arr, "After:  ");
}

// ================================================================
//  3. RECURSIVE FACTORIAL
// ================================================================

long long factorial(int n) {
    if (n <= 1) return 1;           // base case
    return n * factorial(n - 1);   // recursive call — pushes stack frame
}

void test_factorial() {
    std::cout << "\n=== Factorial (recursive) ===\n";

    for (int n = 0; n <= 12; n++) {
        std::cout << "factorial(" << std::setw(2) << n << ") = "
                  << factorial(n) << "\n";
    }
}

// ================================================================
//  4. ITERATIVE FIBONACCI
// ================================================================

long long fibonacci(int n) {
    if (n == 0) return 0;
    if (n == 1) return 1;

    long long prev = 0, curr = 1;
    for (int i = 2; i <= n; i++) {     // tight loop — mirrors fib_loop in MIPS
        long long next = prev + curr;
        prev = curr;
        curr = next;
    }
    return curr;
}

void test_fibonacci() {
    std::cout << "\n=== Fibonacci (iterative) ===\n";

    for (int n = 0; n <= 20; n++) {
        std::cout << "fib(" << std::setw(2) << n << ") = "
                  << fibonacci(n) << "\n";
    }
}

// ================================================================
//  MAIN
// ================================================================

int main() {
    std::cout << "========================================\n";
    std::cout << "  MARS Visualizer Test — C++ Reference\n";
    std::cout << "========================================\n";

    test_arithmetic();
    test_sort();
    test_factorial();
    test_fibonacci();

    std::cout << "\n=== All tests complete! ===\n";
    return 0;
}
