package main

import (
	"time"
	"strconv"
	"fmt"
	"strings"
)

func main() {
	start := time.Now().UnixNano()
	s := ""
	for i := 10000; i > 0; i-- {
		s = s + strconv.Itoa(i);
	}
	fmt.Printf("used mills: %f\n", float32(time.Now().UnixNano() - start) / 1000000.0)

	strs := make([]string, 10000)
	for i := 10000; i > 0; i-- {
		strs[i - 1] = strconv.Itoa(i);
	}
	start = time.Now().UnixNano();
	strings.Join(strs, "")
	fmt.Printf("used mills: %f\n", float32(time.Now().UnixNano() - start) / 1000000.0)
}
