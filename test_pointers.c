int* global_ptr;

int main() {
    int x;
    int* p;
    int y;

    // Test AddressOf and Dereference
    x = 42;
    p = &x;
    *p = 100;
    
    y = *p + 20;

    // Test malloc
    global_ptr = malloc(4);
    *global_ptr = 999;
    
    free(global_ptr);
    
    return y;
}
