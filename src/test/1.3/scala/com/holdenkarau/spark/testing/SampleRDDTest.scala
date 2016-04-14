/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.holdenkarau.spark.testing

import scala.util.Random

import org.apache.spark.rdd.RDD
import org.scalatest.FunSuite

class SampleRDDTest extends FunSuite with SharedSparkContext {
  test("really simple transformation") {
    val input = List("hi", "hi holden", "bye")
    val expected = List(List("hi"), List("hi", "holden"), List("bye"))

    assert(tokenize(sc.parallelize(input)).collect().toList === expected)
  }
  def tokenize(f: RDD[String]) = {
    f.map(_.split(" ").toList)
  }

  test("really simple transformation with rdd - rdd comparision") {
    val inputList = List("hi", "hi holden", "bye")
    val inputRDD = tokenize(sc.parallelize(inputList))

    val expectedList = List(List("hi"), List("hi", "holden"), List("bye"))
    val expectedRDD = sc.parallelize(expectedList)

    assert(None === RDDComparisons.compare(expectedRDD, inputRDD))
  }

  test("RDD comparision with order same partitioner") {
    val inputList = List("hi", "hi holden", "byez")
    val inputRDD = sc.parallelize(inputList)
    val tokenizedRDD = tokenize(inputRDD)
    val ordered1 = tokenizedRDD.sortBy(x => x.head)
    val ordered2 = tokenizedRDD.sortBy(x => x.head)

    assert(None === RDDComparisons.compareWithOrder(ordered1, ordered2))
  }

  test("RDD comparision with order without known partitioner") {
    val inputList = List("hi", "hi holden", "byez")
    val inputRDD = sc.parallelize(inputList)
    val tokenizedRDD = tokenize(inputRDD)
    val ordered = tokenizedRDD.sortBy(x => x.head)

    val expectedList = List(List("hi"), List("hi", "holden"), List("byez"))
    val expectedRDD = sc.parallelize(expectedList).sortBy(x => x.head)
    val diffExpectedRDD = sc.parallelize(expectedList).sortBy(x => x.head.reverse)

    assert(ordered.partitioner.isEmpty && expectedRDD.partitioner.isEmpty)
    assert(None === RDDComparisons.compareWithOrder(expectedRDD, ordered))
    // Different order
    assert(RDDComparisons.compareWithOrder(diffExpectedRDD, ordered).isDefined)
    // Different sizes
    val fakeTokenized = inputRDD.map(x => List(x))
    assert(RDDComparisons.compareWithOrder(diffExpectedRDD, fakeTokenized).isDefined)
    assert(RDDComparisons.compareWithOrder(expectedRDD, fakeTokenized).isDefined)
  }

  test("empty RDD compare") {
    val inputList = List[String]()
    val inputRDD = sc.parallelize(inputList)

    assert(None === RDDComparisons.compare(inputRDD, inputRDD))
  }

  test("simple equal compare") {
    val inputList = List("ab", "bc", "bc", "cd")
    val inputRDD = sc.parallelize(inputList)

    val expected = Random.shuffle(inputList)
    val expectedRDD = sc.parallelize(expected)
    println(expected)

    assert(None === RDDComparisons.compare(expectedRDD, inputRDD))
  }

  test("complex equal compare") {
    val inputList = List(("ab", 4), "bc", "bc", ("cd", 6), ("ab", 4), "hanafy", 55)
    val inputRDD = sc.parallelize(inputList)

    val expected = Random.shuffle(inputList)
    val expectedRDD = sc.parallelize(expected)

    assert(None === RDDComparisons.compare(expectedRDD, inputRDD))
  }

  test("not equal compare") {
    val inputList = List("ab", 1)
    val inputRDD = sc.parallelize(inputList)

    val expectedList = List("ab", -1)
    val expectedRDD = sc.parallelize(expectedList)

    assert(None !== RDDComparisons.compare(expectedRDD, inputRDD))
  }

  test("empty RDD compareWithOrder") {
    val inputList = List[String]()
    val inputRDD = sc.parallelize(inputList)

    assert(None === RDDComparisons.compareWithOrder(inputRDD, inputRDD))
  }

  test("equal compareWithOrder") {
    val inputList = List("ab", "bc", "holden", (1, "wxyz"), 22, "abo trika")
    val inputRDD = sc.parallelize(inputList)

    val expectedRDD = sc.parallelize(inputList)

    assert(None === RDDComparisons.compareWithOrder(inputRDD, expectedRDD))
  }

  test("not equal compareWithOrder") {
    val inputList = List(1, 2, 3, 4)
    val inputRDD = sc.parallelize(inputList)

    val expectedList = List(2, 1, 3, 4)
    val expectedRDD = sc.parallelize(expectedList)

    assert(None !== RDDComparisons.compareWithOrder(inputRDD, expectedRDD))
  }
}
