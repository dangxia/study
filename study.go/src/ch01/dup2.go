package main

import (
	"bufio"
	"os"
	"fmt"
)

func main() {
	counts := make(map[string]int)
	files := os.Args[1:]

	if len(files) == 0 {
		countLines(os.Stdin, counts, true)
	} else {
		for _, file := range files {
			fileReader, err := os.Open(file)
			if err != nil {
				fmt.Fprintf(os.Stderr, "dup2: %v\n", err)
				continue
			}
			countLines(fileReader, counts, false)
			fileReader.Close()
		}
	}
	for line, n := range counts {
		if n > 1 {
			fmt.Printf("line:%s\tcount:%d\n", line, n)
		}
	}
}

func countLines(r *os.File, counts map[string]int, isStdin bool) {
	scanner := bufio.NewScanner(r)
	for scanner.Scan() {
		token := scanner.Text()
		if isEOF(token, isStdin) {
			break
		}
		counts[token]++
	}
}

func isEOF(token string, isStdin bool) bool {
	return isStdin && token == ""
}