.data
# We define an array of 16 values in the data segment.
# The Array Visualizer will watch this memory block.
array:  .word 250, 240, 230, 220, 210, 200, 190, 180, 170, 160, 150, 140, 130, 120, 110, 100
length: .word 16

.text
.globl main
main:
    # Initialize arguments for our function call
    la $a0, array       # $a0 = base address of array
    lw $a1, length      # $a1 = length of the array
    
    # Jump and link to our function (tests Pipeline and Stack $ra)
    jal modify_array
    
    # Exit program cleanly
    li $v0, 10
    syscall

# -------------------------------------------------------------------
# Function: modify_array
# Purpose: Divides every element in the array by 2.
# Demonstrates: Stack frames (prologue/epilogue) and Array writes.
# -------------------------------------------------------------------
modify_array:
    # === FUNCTION PROLOGUE (Tests Stack Visualizer) ===
    # Allocate 16 bytes (4 words) on the stack
    subu $sp, $sp, 16
    sw $ra, 12($sp)     # Save return address
    sw $s0, 8($sp)      # Save $s0
    sw $s1, 4($sp)      # Save $s1
    sw $s2, 0($sp)      # Save $s2
    
    move $s0, $a0       # $s0 = array pointer
    move $s1, $a1       # $s1 = length
    li $s2, 0           # $s2 = loop counter (i = 0)
    
loop_start:
    bge $s2, $s1, loop_end  # if (i >= length) exit loop
    
    # Let's do a temporary stack push/pop inside the loop just to make
    # the Stack Visualizer "bounce" up and down repeatedly!
    subu $sp, $sp, 4
    sw $s2, 0($sp)      # Push 'i' to the stack
    
    # Read array[i]
    sll $t0, $s2, 2     # i * 4
    add $t1, $s0, $t0   # address = base + (i * 4)
    lw $t2, 0($t1)      # Load array[i]
    
    # Modify the value (Divide by 2)
    srl $t2, $t2, 1
    
    # Write back to array 
    # (THIS WILL TRIGGER THE ARRAY VISUALIZER TO GLOW RED)
    sw $t2, 0($t1)      
    
    # Pop from our temporary stack
    lw $t3, 0($sp)
    addu $sp, $sp, 4
    
    # Increment i and loop (Tests Pipeline Visualizer branching)
    addi $s2, $s2, 1
    j loop_start
    
loop_end:
    # === FUNCTION EPILOGUE (Tests Stack Visualizer) ===
    # Restore saved registers
    lw $ra, 12($sp)
    lw $s0, 8($sp)
    lw $s1, 4($sp)
    lw $s2, 0($sp)
    
    # Deallocate stack frame
    addu $sp, $sp, 16
    
    jr $ra              # Return to caller
