package com.simplaex.io


object CsvHelper {
	implicit class CSVWrapper(val prod: Product) extends AnyVal {
		def toCSV: String = prod.productIterator.map{
			case Some(value) => value
			case None => ""
			case rest => rest
		}.mkString(",")
}

}
