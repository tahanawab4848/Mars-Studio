# test_all_tools.asm
# A program designed to exercise all MARS visualizer tools simultaneously.
#
# Features demonstrated:
# 1. Pipeline Visualizer: Branching, ALU operations, arithmetic loops.
# 2. Array Visualizer: Bubble sort algorithm reading/writing memory.
# 3. Stack Visualizer: Recursive factorial function pushing/popping frames.

.data
    # For Array Sorting Visualizer (Base Address 0x10010000)
    arr: .word 42, 7, 99, 3, 55, 18, 71, 26, 84, 11
    arr_size: .word 10

.text
.globl main

main:
    # ----------------------------------------------------------------
    # 1. Arithmetic & Loops (Pipeline Visualizer)
    # ----------------------------------------------------------------
    li $t0, 100
    li $t1, 37
    add $t2, $t0, $t1
    sub $t2, $t0, $t1
    mul $t2, $t0, $t1
    
    li $t3, 0
arith_loop:
    beq $t3, 5, arith_done
    add $t2, $t2, $t0
    addi $t3, $t3, 1
    j arith_loop
arith_done:

    # ----------------------------------------------------------------
    # 2. Bubble Sort (Array Visualizer)
    # ----------------------------------------------------------------
    la $a0, arr
    lw $a1, arr_size
    jal bubble_sort
    
    # ----------------------------------------------------------------
    # 3. Recursive Factorial (Stack Visualizer)
    # ----------------------------------------------------------------
    li $a0, 6             # Calculate 6! (720)
    jal factorial
    
    # Exit
    li $v0, 10
    syscall

# ====================================================================
# Function: bubble_sort
# Args: $a0 = base address of array, $a1 = number of elements
# ====================================================================
bubble_sort:
    li $t0, 0             # i = 0
outer_loop:
    subi $t1, $a1, 1      # n - 1
    bge $t0, $t1, sort_done
    li $t2, 0             # j = 0
    sub $t3, $t1, $t0     # n - 1 - i
inner_loop:
    bge $t2, $t3, inner_done
    
    sll $t4, $t2, 2
    add $t4, $a0, $t4     # arr[j] addr
    lw $t5, 0($t4)        # arr[j]
    lw $t6, 4($t4)        # arr[j+1]
    
    ble $t5, $t6, skip_swap
    
    # Swap
    sw $t6, 0($t4)        # writes to memory (Array Visualizer highlights)
    sw $t5, 4($t4)
    
skip_swap:
    addi $t2, $t2, 1
    j inner_loop
inner_done:
    addi $t0, $t0, 1
    j outer_loop
sort_done:
    jr $ra

# ====================================================================
# Function: factorial
# Args: $a0 = n
# Returns: $v0 = n!
# ====================================================================
factorial:
    # Prologue: allocate stack frame
    addi $sp, $sp, -8     # (Stack Visualizer highlights push)
    sw $ra, 4($sp)
    sw $a0, 0($sp)
    
    li $t0, 1
    ble $a0, $t0, fact_base
    
    # Recursive call: factorial(n - 1)
    addi $a0, $a0, -1
    jal factorial
    
    # Calculate: n * factorial(n - 1)
    lw $a0, 0($sp)
    mul $v0, $v0, $a0
    j fact_end
    
fact_base:
    li $v0, 1
    
fact_end:
    # Epilogue: deallocate stack frame
    lw $ra, 4($sp)
    addi $sp, $sp, 8      # (Stack Visualizer highlights pop)
    jr $ra
