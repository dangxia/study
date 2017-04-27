package main

import (
	"fmt"
	//"ch1/a"
	"ch1/b"
)


/*
first comment
second
 */
func main() {
	//line comment
	fmt.Println("Hello 世界")
	fmt.Println("Hello", "世界")
	fmt.Println('世') // console show 19990,because rune is numeric constant
	fmt.Printf("%c\n", '你')
	fmt.Printf("%q\n", '你')
	fmt.Printf("%+q\n", '你')

	var t0 b.T0 = "sdjlf"
	//t1 := ""
	//t0 = t1
	//var t0 *T0
	//t1 := new(T1)
	//t0 = t1
	fmt.Println(t0)

	s := make([]int, 0, 2)
	s = append(s, 1)
	s = append(s, 2)
	s = append(s, 3)
	fmt.Println(s)

}
