package main

import (
	"bufio"
	"os"
	"fmt"
)

func main() {
	counts := make(map[string]int);
	input := bufio.NewScanner(os.Stdin)

	for input.Scan() {
		item := input.Text()
		if item == "" {
			break;
		}
		counts[item]++
	}

	for key, value := range counts {
		if value > 1 {
			fmt.Printf("%d\t%s\n", value, key)
		}
	}
}
