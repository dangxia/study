package html

import (
	"net/http"
	"fmt"
	"syscall"
	"io/ioutil"
	"regexp"
	"strings"
	"os"
	"io"
)

type Node struct {
	Url    string
	IsLeaf bool
	Name   string
	List   *[]*Node
}

func (node *Node) GetList() *[]*Node {
	if node.List == nil {
		node.List = node.fetchList()
	}
	return node.List
}

func (node *Node) fetchList() *[]*Node {
	resp, error := http.Get(node.Url)
	if error != nil {
		fmt.Println("fetch error")
		syscall.Exit(1)
	}
	defer resp.Body.Close()
	body, error := ioutil.ReadAll(resp.Body)
	if error != nil {
		fmt.Println("body error")
		syscall.Exit(1)
	}

	str := string(body);
	rg, error := regexp.Compile(`<td><a\s+href="([^\.][^"]+)"\s*>([^<]+)<.*?</td>`)
	result := make([]*Node, 0)
	for _, item := range rg.FindAllStringSubmatch(str, len(str)) {
		inner := item[2]
		url := item[1]
		isLeaf := !strings.HasSuffix(inner, "/")
		var name string
		if isLeaf {
			name = url[strings.LastIndex(url, "/") + 1:]
		} else {
			strs := strings.Split(url, "/")
			name = strs[len(strs) - 2]
		}
		if strings.HasSuffix(name,`SNAPSHOT`) {
			continue
		}
		node := Node{Url: url, Name: name, IsLeaf: isLeaf}
		result = append(result, &node)
	}
	return &result
}

func (node *Node) DownLoad(basePath string, depth int) {
	if node.IsLeaf {
		out, err := os.Create(basePath + string(os.PathSeparator) + node.Name)
		defer out.Close()
		if (err != nil) {
			fmt.Println("create file error", err)
			os.Exit(1)
		}
		resp, err := http.Get(node.Url)
		defer resp.Body.Close()
		_, err = io.Copy(out, resp.Body)
	} else {
		depth := depth + 1
		childPath := basePath + string(os.PathSeparator) + node.Name
		os.MkdirAll(childPath, 0755)
		if depth == 2 {
			fmt.Println("download...", childPath)
		}
		for _, child := range *(node.GetList()) {
			child.DownLoad(childPath, depth)
		}
	}

}
