package main

import (
	"os"
	"fmt"
)

func main() {
	s, sep := "", ""
	for _, arg := range os.Args[1:] {
		s = s + sep + arg
		sep = " "
	}
	fmt.Println(s)
}
