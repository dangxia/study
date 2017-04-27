package main

import (
	"os"
	"bufio"
	"fmt"
)

func main() {
	counts := make(map[string]map[string]int)
	for _, file := range os.Args[1:] {
		fileHd, _ := os.Open(file)
		countLines2(fileHd, counts)
		fileHd.Close()
	}

	for token, set := range counts {
		total := 0
		for _, count := range set {
			total += count
		}
		if total <= 1 {
			continue
		}
		fmt.Printf("token: %q \n", token)
		for fileName, count := range set {
			fmt.Printf("\tfile: %s, count: %d\n", fileName, count)
		}
		fmt.Printf("total: %d\n", total)
		fmt.Println("--------------------------------")
	}
}

func countLines2(file *os.File, counts map[string]map[string]int) {
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		token := scanner.Text()
		set := counts[token]
		if set == nil {
			set = make(map[string]int)
			counts[token] = set
		}
		set[file.Name()]++
	}
}
