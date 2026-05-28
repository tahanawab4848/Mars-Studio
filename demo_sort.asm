.data
# A simple array of 16 integers to sort
array:  .word   45, 12, 89, 3, 56, 23, 91, 15, 67, 34, 78, 9, 82, 41, 62, 28
length: .word   16

.text
.globl main
main:
    # Bubble Sort implementation
    la $t0, array       # $t0 = base address of array
    lw $t1, length      # $t1 = length of array
    
    # Outer loop counter i
    li $t2, 0           
outer_loop:
    bge $t2, $t1, end_sort
    
    # Inner loop counter j
    li $t3, 0           
    sub $t4, $t1, $t2   # $t4 = length - i - 1
    subi $t4, $t4, 1
    
inner_loop:
    bge $t3, $t4, next_outer
    
    # Calculate addresses
    sll $t5, $t3, 2     # j * 4
    add $t6, $t0, $t5   # addr of array[j]
    
    lw $t7, 0($t6)      # $t7 = array[j]
    lw $t8, 4($t6)      # $t8 = array[j+1]
    
    # Compare
    ble $t7, $t8, next_inner
    
    # Swap
    sw $t8, 0($t6)
    sw $t7, 4($t6)
    
next_inner:
    addi $t3, $t3, 1
    j inner_loop
    
next_outer:
    addi $t2, $t2, 1
    j outer_loop
    
end_sort:
    # Exit program
    li $v0, 10
    syscall
