package main

import (
	"hehe/html"
	"os"
)

func main() {
	//fmt.Println(html.List("http://nexus.bigdata.letv.com/nexus/content/groups/public/le/data/bdp/"))
	os.MkdirAll("/tmp/hexh/sdf", 0755)
	p := html.Node{Url:"http://nexus.bigdata.letv.com/nexus/content/groups/public/le/data/bdp/", IsLeaf:false}
	p.DownLoad("/tmp/hexh/sdf",0)
}
