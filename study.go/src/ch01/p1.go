package main

import (
	"os"
	"fmt"
	"strconv"
)

func main() {
	s, sep := "", ""
	for index, arg := range os.Args[1:] {
		s = s + sep + strconv.Itoa(index) + "\t" + arg
		sep = "\n"
	}
	fmt.Println(s)
}
