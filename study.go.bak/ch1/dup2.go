package main

import (
	"os"
	"bufio"
	"fmt"
)

func main() {
	counts := make(map[string]int)
	if len(os.Args[1:]) == 0 {
		countLines(os.Stdin, counts, true)
	} else {
		for _, arg := range os.Args[1:] {
			f, err := os.Open(arg)
			if err != nil {
				fmt.Fprintf(os.Stderr, "open file %s failed,%v\n", arg, err)
				continue
			}
			countFileLines(f, counts)
		}
	}

	for line, count := range counts {
		if count > 1 {
			fmt.Printf("line: %s, count: %d\n", line, count)
		}
	}
}

func countFileLines(file *os.File, counts map[string]int) {
	countLines(file, counts, false)
}

func countLines(file *os.File, counts map[string]int, isStdin bool) {
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		text := scanner.Text();
		if isStdin && text == "quit" {
			break
		}
		counts[text]++
	}
}
