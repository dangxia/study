package main

import (
	"sync"
	"net/http"
	"log"
	"fmt"
)

var count int
var mu sync.Mutex

func main() {
	http.HandleFunc("/", handler2)
	http.HandleFunc("/count", counter)
	http.HandleFunc("/favicon.ico", empty)

	log.Fatal(http.ListenAndServe("localhost:8000", nil))
}

func handler2(w http.ResponseWriter, r *http.Request) {
	fmt.Println("---------handler2-------------")
	mu.Lock()
	count++
	fmt.Printf("count:%d\n", count)
	mu.Unlock()
	fmt.Fprintf(w, "URL.PATH = %q\n", r.URL.Path)
}

func counter(w http.ResponseWriter, r *http.Request) {
	fmt.Println("---------counter-------------")
	mu.Lock()
	fmt.Fprintf(w, "Count %d\n", count)
	mu.Unlock()
}

func empty(_ http.ResponseWriter, _ *http.Request) {
}


