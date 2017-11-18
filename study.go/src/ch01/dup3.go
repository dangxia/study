package main

import (
	"os"
	"io/ioutil"
	"fmt"
	"strings"
)

func main() {
	counts := make(map[string]int)
	for _, file := range os.Args[1:] {
		data, err := ioutil.ReadFile(file)
		if err != nil {
			fmt.Printf("dup3 error %s %v", file, err)
			continue
		}

		for _, line := range strings.Split(string(data), "\n") {
			counts[line]++
		}
	}

	for line, n := range counts {
		if n > 1 {
			fmt.Printf("line: %s, count: %d\n", line, n)
		}
	}

}
