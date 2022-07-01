package com.studiomath.innernotes.document.path

/**
 * Clip driver
 * @param  {Array.<Array.<Number>>} polygonA
 * @param  {Array.<Array.<Number>>} polygonB
 * @param  {Boolean}                sourceForwards
 * @param  {Boolean}                clipForwards
 * @return {Array.<Array.<Number>>}
 */
fun polygonClippingAlgorithm (polygonA: MutableList<MutableList<Double>>, polygonB: MutableList<MutableList<Double>>, eA: Boolean, eB: Boolean): MutableList<MutableList<MutableList<Double>>> {
    val source = Polygon(polygonA)
    val clip = Polygon(polygonB)
    return source.clip(clip, eA, eB)
}



class Polygon {
    private var _firstIntersect: Vertex? = null
    private var _lastUnprocessed: Vertex?
    var first: Vertex?
    private var vertices: Int

    /**
     * Polygon representation
     * @param {Array.<Array.<Number>>} p
     * @param {Boolean=}               arrayVertices
     */
    constructor (p: MutableList<MutableList<Double>>) {

        /**
         * @type {Vertex}
         */
        this.first = null

        /**
         * @type {Number}
         */
        this.vertices = 0

        /**
         * @type {Vertex}
         */
        this._lastUnprocessed = null

        /**
         * Whether to handle input and output as [x,y] or {x:x,y:y}
         * @type {Boolean}
         *
         * Aggiornato: solo [x,y]
         */
        // TODO: 10/12/2021 rimuovo la possibilità di scegliere tra due tipologie di dati
        for (i in p) {
            this.addVertex(Vertex(i[0], i[1]))
        }
    }


    /**
     * Add a vertex object to the polygon
     * (vertex is added at the 'end' of the list')
     *
     * @param vertex
     */
     fun addVertex (vertex: Vertex) {
        if (this.first == null) {
            this.first      = vertex
            this.first!!.next = vertex
            this.first!!.prev = vertex
        } else {
            val next = this.first
            val prev = next!!.prev

            next.prev   = vertex
            vertex.next = next
            vertex.prev = prev
            prev!!.next   = vertex
        }
        this.vertices++
    }


    /**
     * Inserts a vertex inbetween start and end
     *
     * @param {Vertex} vertex
     * @param {Vertex} start
     * @param {Vertex} end
     */
    fun insertVertex (vertex: Vertex, start: Vertex, end: Vertex) {
        var prev: Vertex
        var curr = start

        while (!curr.equals(end) && curr._distance < vertex._distance) {
            curr = curr.next!!
        }

        vertex.next = curr
        prev        = curr.prev!!

        vertex.prev = prev
        prev.next   = vertex
        curr.prev   = vertex

        this.vertices++
    }

    /**
     * Get next non-intersection point
     * @param  {Vertex} v
     * @return {Vertex}
     */
    fun getNext (v: Vertex): Vertex {
        var c = v
        while (c._isIntersection) c = c.next!!
        return c
    }


    /**
     * Unvisited intersection
     * @return {Vertex}
     */
    fun getFirstIntersect (): Vertex {
        var v = if(this._firstIntersect != null) this._firstIntersect else this.first

        do {
            if (v!!._isIntersection && !v._visited) break

            v = v.next
        } while (v != this.first)

        this._firstIntersect = v
        return v!!
    }


    /**
     * Does the polygon have unvisited vertices
     * @return {Boolean} [description]
     */
    fun hasUnprocessed(): Boolean {
        var v = if (this._lastUnprocessed != null) this._lastUnprocessed else this.first
        do {
            if (v!!._isIntersection && !v._visited) {
                this._lastUnprocessed = v
                return true
            }

            v = v.next
        } while (v != this.first)

        this._lastUnprocessed = null
        return false
    }


    /**
     * The output depends on what you put in, arrays or objects
     * @return {Array.<Array<Number>|Array.<Object>}
     */
    fun getPoints(): MutableList<MutableList<Double>> {
        val points = mutableListOf<MutableList<Double>>()
        var v = this.first!!

        do {
            points.add(mutableListOf(v.x, v.y))
            v = v.next!!
        } while (v != this.first)


//        if (this._arrayVertices) {
//
//        } else {
//            do {
//                points.push({
//                        x: v.x,
//                        y: v.y
//                });
//                v = v.next
//            } while (v !== this.first)
//        }

        return points
    }

    /**
     * Clip polygon against another one.
     * Result depends on algorithm direction:
     *
     * Intersection: forwards forwards
     * Union:        backwars backwards
     * Diff:         backwards forwards
     *
     * @param {Polygon} clip
     * @param {Boolean} sourceForwards
     * @param {Boolean} clipForwards
     */
    fun clip (clip: Polygon, sourceForwards: Boolean, clipForwards: Boolean): MutableList<MutableList<MutableList<Double>>> {
        var sourceForwards = sourceForwards
        var clipForwards = clipForwards

        var sourceVertex = this.first!!
        var clipVertex = clip.first!!
//        var sourceInClip
//        var clipInSource

        val isUnion        = !sourceForwards && !clipForwards;
        val isIntersection = sourceForwards && clipForwards;
        val isDiff         = !isUnion && !isIntersection;

        // calculate and mark intersections
        do {
            if (!sourceVertex._isIntersection) {
                do {
                    if (!clipVertex._isIntersection) {
                        val i = Intersection(
                            sourceVertex,
                            this.getNext(sourceVertex.next!!),
                            clipVertex, clip.getNext(clipVertex.next!!)
                        );

                        if (i.valid()) {
                            val sourceIntersection = Vertex.createIntersection(i.x, i.y, i.toSource)
                            val clipIntersection   = Vertex.createIntersection(i.x, i.y, i.toClip)

                            sourceIntersection._corresponding = clipIntersection;
                            clipIntersection._corresponding   = sourceIntersection;

                            this.insertVertex(sourceIntersection, sourceVertex, this.getNext(sourceVertex.next!!))
                            clip.insertVertex(clipIntersection, clipVertex, clip.getNext(clipVertex.next!!))
                        }
                    }
                    clipVertex = clipVertex.next!!
                } while (!clipVertex.equals(clip.first))
            }

            sourceVertex = sourceVertex.next!!
        } while (!sourceVertex.equals(this.first))

        // phase two - identify entry/exit points
        sourceVertex = this.first!!
        clipVertex   = clip.first!!

        var sourceInClip = sourceVertex.isInside(clip)
        var clipInSource = clipVertex.isInside(this)

        // bitwise xor, ^=
        sourceForwards = sourceInClip.let { sourceForwards.xor(it) } == true
        clipForwards = clipInSource.let { clipForwards.xor(it) } == true

        do {
            if (sourceVertex._isIntersection) {
                sourceVertex._isEntry = sourceForwards;
                sourceForwards = !sourceForwards;
            }
            sourceVertex = sourceVertex.next!!
        } while (sourceVertex != this.first)

        do {
            if (clipVertex._isIntersection) {
                    clipVertex._isEntry = clipForwards
                    clipForwards = !clipForwards
                }
            clipVertex = clipVertex.next!!
        } while (clipVertex != clip.first)

        // phase three - construct a list of clipped polygons
        var list = mutableListOf<MutableList<MutableList<Double>>>()

        while (this.hasUnprocessed()) {
            var current = this.getFirstIntersect()
            // keep format
            val clipped = Polygon(mutableListOf())

            clipped.addVertex(Vertex(current.x, current.y))
            do {
                current.visit()
                if (current._isEntry) {
                    do {
                        current = current.next!!
                        clipped.addVertex(Vertex(current.x, current.y))
                    } while (!current._isIntersection);

                } else {
                    do {
                        current = current.prev!!
                        clipped.addVertex(Vertex(current.x, current.y))
                    } while (!current._isIntersection)
                }
                current = current._corresponding!!
            } while (!current._visited)

            list.add(clipped.getPoints())
        }

        if (list.size == 0) {
            if (isUnion) {
                if (sourceInClip) list.add(clip.getPoints())
                else if (clipInSource) list.add(this.getPoints())
                else {
                    list.add(this.getPoints())
                    list.add(clip.getPoints())
                }
            } else if (isIntersection) { // intersection
                if (sourceInClip) list.add(this.getPoints())
                else if (clipInSource) list.add(clip.getPoints())
            } else { // diff
                if (sourceInClip) {
                    list.add(clip.getPoints())
                    list.add(this.getPoints())
                }
                else if (clipInSource) {
                    list.add(this.getPoints())
                    list.add(clip.getPoints())
                }
                else list.add(this.getPoints())
            }
            // TODO: 10/12/2021 non penso che questa istruzione servi
            //if (list.size == 0) list = null
        }

        return list
    }
}



class Vertex {

    var _visited: Boolean
    var _isIntersection: Boolean
    var _isEntry: Boolean
    var _distance: Double
    var _corresponding: Vertex?

    var prev: Vertex?
    var next: Vertex?
    var y: Double
    var x: Double

    /**
     * Vertex representation
     *
     * @param {Number|Array.<Number>} x
     * @param {Number=}               y
     */
    constructor (x: Double, y: Double) {
//        if (arguments.length == 1) {
//            // Coords
//            if (Array.isArray(x)) {
//                y = x[1];
//                x = x[0];
//            } else {
//                y = x.y;
//                x = x.x;
//            }
//        }

        /**
         * X coordinate
         * @type {Number}
         */
        this.x = x

        /**
         * Y coordinate
         * @type {Number}
         */
        this.y = y

        /**
         * Next node
         * @type {Vertex}
         */
        this.next = null

        /**
         * Previous vertex
         * @type {Vertex}
         */
        this.prev = null

        /**
         * Corresponding intersection in other polygon
         */
        this._corresponding = null

        /**
         * Distance from previous
         */
        this._distance = 0.0

        /**
         * Entry/exit point in another polygon
         * @type {Boolean}
         */
        this._isEntry = true

        /**
         * Intersection vertex flag
         * @type {Boolean}
         */
        this._isIntersection = false

        /**
         * Loop check
         * @type {Boolean}
         */
        this._visited = false
    }


    companion object{
        /**
         * Creates intersection vertex
         * @param  {Number} x
         * @param  {Number} y
         * @param  {Number} distance
         * @return {Vertex}
         */
        fun createIntersection (x: Double, y: Double, distance: Double): Vertex {
            val vertex = Vertex(x, y)
            vertex._distance = distance
            vertex._isIntersection = true
            vertex._isEntry = false
            return vertex
        }
    }


    /**
     * Mark as visited
     */
    fun visit () {
        this._visited = true
        // TODO: 10/12/2021 credo di poter togliere this._corresponding != null
        if (this._corresponding != null && !this._corresponding!!._visited) {
            this._corresponding?.visit()
        }
    }


    /**
     * Convenience
     * @param  {Vertex}  v
     * @return {Boolean}
     */
    fun equals (v: Vertex): Boolean {
        return this.x == v.x && this.y == v.y
    }


    /**
     * Check if vertex is inside a polygon by odd-even rule:
     * If the number of intersections of a ray out of the point and polygon
     * segments is odd - the point is inside.
     * @param {Polygon} poly
     * @return {Boolean}
     */
    fun isInside (poly: Polygon): Boolean {
        var oddNodes = false
        var vertex = poly.first
        var next = vertex?.next
        val x = this.x
        val y = this.y

        do {
            if (vertex != null) {
                if (next != null) {
                    if ((vertex.y < y && next.y   >= y ||
                                next.y < y && vertex.y >= y) &&
                        (vertex.x <= x || next.x <= x)) {
                        oddNodes = oddNodes.xor(vertex.x + (y - vertex.y) / (next.y - vertex.y) * (next.x - vertex.x) < x)
                    }
                }
            }

            if (vertex != null) {
                vertex = vertex.next
            }
            if (vertex != null) {
                next = if(vertex.next != null) vertex.next else poly.first
            }
        } while (vertex != poly.first)

        return oddNodes
    }
}



class Intersection {
    var toClip: Double
    var toSource: Double
    var y: Double
    var x: Double

    /**
     * @param {Vertex} s1
     * @param {Vertex} s2
     * @param {Vertex} c1
     * @param {Vertex} c2
     */
    constructor(s1: Vertex, s2: Vertex, c1: Vertex, c2: Vertex) {

        /**
         * @type {Number}
         */
        this.x = 0.0

        /**
         * @type {Number}
         */
        this.y = 0.0

        /**
         * @type {Number}
         */
        this.toSource = 0.0

        /**
         * @type {Number}
         */
        this.toClip = 0.0

        var d = (c2.y - c1.y) * (s2.x - s1.x) - (c2.x - c1.x) * (s2.y - s1.y)

        if (d == 0.0) return

        /**
         * @type {Number}
         */
        this.toSource = ((c2.x - c1.x) * (s1.y - c1.y) - (c2.y - c1.y) * (s1.x - c1.x)) / d

        /**
         * @type {Number}
         */
        this.toClip = ((s2.x - s1.x) * (s1.y - c1.y) - (s2.y - s1.y) * (s1.x - c1.x)) / d

        if (this.valid()) {
            this.x = s1.x + this.toSource * (s2.x - s1.x)
            this.y = s1.y + this.toSource * (s2.y - s1.y)
        }
    }


    /**
     * @return {Boolean}
     */
    fun valid (): Boolean {
        return (0 < this.toSource && this.toSource < 1) && (0 < this.toClip && this.toClip < 1)
    }
}