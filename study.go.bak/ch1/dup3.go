package main

import (
	"os"
	"fmt"
	"io/ioutil"
	"strings"
)

func main() {
	if (len(os.Args) == 1) {
		fmt.Println("not found file name")
		return
	}
	counts := make(map[string]int)

	for _, filename := range os.Args[1:] {
		data, err := ioutil.ReadFile(filename)
		if err != nil {
			fmt.Printf("open file:%s error,%v\n", filename, err)
			return
		}
		for _, line := range strings.Split(string(data), "\n") {
			counts[line]++
		}
	}

	for line, count := range counts {
		if count > 1 {
			fmt.Printf("line: %s count: %d\n", line, count)
		}
	}
}
