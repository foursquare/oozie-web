function loadData(){
    // grab wf id from url
    var url = document.URL;
    var wfid = url.substring(url.lastIndexOf("/") + 1);
    var definitionUrl = "/workflows/"+wfid+"/def";
    var result = $.ajax({ url : definitionUrl, success : makeGraph});
}

function newGraph(nodes, edges) {
    return { 'nodes' : nodes, 'edges' : edges};
}

function copyGraph( graph ) {
    return JSON.decode(JSON.encode(graph));
}

function buildDag(wfdef) {
    var elems = wfdef.childNodes[0].childNodes;
    var nodes = {};
    var edges = Array();
    var createEdge = function(from, to, kind){
        edges.push( edge( from, to, kind) );
        from.out.push(to);
        to.in.push(from);
    }
    for(var i=0;i<elems.length;i++){
        var c = elems[i];
        if(c.nodeName == "#text") {
            continue;
        } else if(c.tagName == "action"){
            var name = c.getAttribute("name");
            nodes[name] = node(name);
        } else if(c.tagName == "fork") {
            var name = c.getAttribute("name");
            nodes[name] = node(name);
        } else if(c.tagName == "join") {
            var name = c.getAttribute("name");
            nodes[name] = node(name);
        } else if(c.tagName == "start") {
          nodes["start"] = node("start");
        } else if(c.tagName == "end") {
            nodes["end"] = node("end");
        } else if (c.tagName == "kill") {
            var name = c.getAttribute("name");
            nodes[name] = node(name);
        }
    }
    for(var i=0;i<elems.length;i++){
      var c = elems[i];
      if(c.nodeName == "#text") {
          continue;
      } else if(c.tagName=="start"){
          createEdge( nodes["start"], nodes[c.getAttribute("to")], 'ok');          
      } else if(c.tagName == "action"){
          var name = c.getAttribute("name");
          var ok = c.getElementsByTagName("ok")[0]; // actions must have exactly 1 ok tag, I think
          var next = ok.getAttribute("to");
          createEdge( nodes[name], nodes[next], 'ok' );
          var error = c.getElementsByTagName("error")[0];
          var afterError = error.getAttribute("to");
          if(afterError != next){
              createEdge( nodes[name], nodes[afterError], 'error');
          }
      } else if(c.tagName == "fork") {
          var name = c.getAttribute("name");
          var paths = c.getElementsByTagName("path");
          for(var j=0;j<paths.length;j++){
            var path = paths[j];
            createEdge(nodes[name], nodes[path.getAttribute("start")], 'ok');
          }
      } else if(c.tagName == "join") {
          var name = c.getAttribute("name");
          createEdge(nodes[name], nodes[c.getAttribute("to")], 'ok');
      }     
    }
    return newGraph(nodes, edges);    
}

// take a dag which is based on the order in which
// we grabbed stuff out of xml, and instead
// form total ordering consistent with the stcucture
function linearize( graph ){
    var ordered = Array();
    var visit = function(node){
        if(!node['marked']){
            node['marked'] = true;
            node['in'].forEach (
                function(parent) {
                    visit(parent);
                }
            )
            ordered.push(node);
        }
    }   
    var nodes = graph['nodes'];
    for(nodename in nodes){
        var node = nodes[nodename];
        visit(node);
    }
    return ordered;
}

// used to figure out y coordinates
function findLayers(ordered, ymax, m) {
    var depth = {};
    var deepest = -1;
    var layers = Array();
    for(var i=0;i<ordered.length;i++){
        var node = ordered[i];
        node['marked'] = false; // unmark for next step; leaky
        var max = -1;
        for(var j=0;j<node.in.length;j++){
            var parent = node.in[j];
            if(depth[parent.name] > max){
                max = depth[parent.name];
            }            
        }        
        depth[node.name] = 1 + max; // 0 for start
        deepest = (depth[node.name] > deepest) ? depth[node.name] : deepest;
        if(layers.length <= depth[node.name]) {
            layers.push(Array(node));
        } else {
            layers[depth[node.name]].push(node);
        }
    }
    for(var i=0;i<ordered.length;i++){
        var node = ordered[i];
        node.y = m +  ymax * ( depth[node.name] + 0.0) / deepest;
    }
    return { 'depthtable' : depth, 'layers' : layers, 'nodes' : ordered };
}

// used to figure out x coordinates
function findPaths( layered, xmax , m){
    var depthtable = layered['depthtable'];
    var ordered = layered['nodes'];
    var layers = layered['layers'];
    var occupied = Array();
    // do dfs paths through the graph, assigning x values
    for(var i=0;i<ordered.length;i++){
        var node = ordered[i];
        var depth = depthtable[node.name];
        var numAtLevel = layers[depth].length;
        if(occupied.length <= depth){
            occupied.push(1);
        } else {
            occupied[depth] ++;
        }
        node.x = m + xmax * ( occupied[depth] + 0.0) / (numAtLevel + 1);        
        node['fixed'] = true;
    }
    return ordered;       
}

function edge(from, to, kind){
    var e = Object();
    if(! from || ! to){
        throw "hit an undefined or null!"
    }
    e['source'] = from;
    e['target'] = to;
    e['weight'] = 1.0;
    e['kind'] = kind;
    return e;
}

function node(name) {
    var n = {};
    n['name'] = name;
    n['in'] = Array();
    n['out'] = Array();
    return n;
}


function makeGraph(wfdef) {
    var w = 1000, h = 1500, m=100;
    var dag = buildDag(wfdef);
    var linearized = linearize( dag );
    var layered = findLayers(linearized, h, m);
    linearized = findPaths(layered, w, m);
    var nodes = dag['nodes'];
    var edges = dag['edges'];
  
    // what is the longest path start to sink
    
    
    var svg = d3.select("body").append("svg:svg")
        .attr("width", w + 2*m)
        .attr("height", h + 2*m);

    var force = d3.layout.force()
        .size([w, h])
        .linkDistance(1)
        .charge(-1000)
        .gravity(0.1)
        .theta(0.8)
        .on("tick", tick)
        .nodes(d3.values(nodes))
        .links(edges) 
        .start();

    // Per-type markers, as they don't inherit styles.
    svg.append("svg:defs").selectAll("marker")
        .data(["suit", "licensing", "resolved"])
        .enter().append("svg:marker")
        .attr("id", String)
        .attr("viewBox", "0 -5 10 10")
        .attr("refX", 15)
        .attr("refY", -1.5)
        .attr("markerWidth", 6)
        .attr("markerHeight", 6)
        .attr("orient", "auto")
        .append("svg:path");
          
    var path = svg.append("svg:g").selectAll("path")
        .data(force.links())
        .enter().append("svg:path")
        .attr("class", function(d) { return "link " + d.type; })
        .attr("marker-end", function(d) { return "url(#" + d.type + ")"; })
        .style("stroke", function(d ) {
            if(d.kind == "ok") {
                return "green";
            } else {
                return "red"
            }
        })
        .style("fill", "none");
    
    var circle = svg.append("svg:g").selectAll("circle")
        .data(force.nodes())
        .enter().append("svg:circle")
        .attr("r", 6)
        .call(force.drag)
        .style("fill", "gray");
    
    var text = svg.append("svg:g").selectAll("g")
        .data(force.nodes())
        .enter().append("svg:g");
    
    // A copy of the text with a thick white stroke for legibility.
/*
    text.append("svg:text")
        .attr("x", 8)
        .attr("y", ".31em")
        .attr("class", "shadow")
        .text(function(d) { return d.name; });*/
    
    text.append("svg:text")
        .attr("x", 8)
        .attr("y", ".31em")
        .attr("text-anchor", "middle")
        .text(function(d) { return d.name; })
        .attr("transform", "rotate(-45)");
    
    // Use elliptical arc path segments to doubly-encode directionality.
    /* function tick() {
        link.attr("x1", function(d) { return d.source.x; })
            .attr("y1", function(d) { return d.source.y; })
            .attr("x2", function(d) { return d.target.x; })
            .attr("y2", function(d) { return d.target.y; });
        
        node.attr("cx", function(d) { return d.x; })
            .attr("cy", function(d) { return d.y; });
    } */
    
    function tick() {
        path.attr("d", function(d) {
            var dx = d.target.x - d.source.x,
            dy = d.target.y - d.source.y,
            dr = 0;
            return "M" + d.source.x + "," + d.source.y + "A" + dr + "," + dr + " 0 0,1 " + d.target.x + "," + d.target.y;
        });
        
        circle.attr("transform", function(d) {
            return "translate(" + d.x + "," + d.y + ")";
        });
        
        text.attr("transform", function(d) {
            return "translate(" + d.x + "," + d.y + ")";
        });
    }

    
}
