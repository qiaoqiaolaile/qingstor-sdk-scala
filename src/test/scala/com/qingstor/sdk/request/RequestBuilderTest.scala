package com.qingstor.sdk.request

import akka.http.scaladsl.model.{HttpEntity, HttpMethods}
import com.qingstor.sdk.annotation.ParamAnnotation
import com.qingstor.sdk.config.QSConfig
import com.qingstor.sdk.constant.QSConstants
import com.qingstor.sdk.model.QSModels.{Input, Operation}
import org.scalatest.FunSuite
import java.io.File

class RequestBuilderTest extends FunSuite {
  case class TestInput(f: String = null, b: Int, d: String) extends Input {
    @ParamAnnotation(location = QSConstants.ParamsLocationHeader, name = "foo")
    def foo: String = f

    @ParamAnnotation(location = QSConstants.ParamsLocationParam, name = "bar")
    def bar: Int = b

    @ParamAnnotation(location = QSConstants.ParamsLocationHeader, name = "Date")
    def date: String = d
  }

  test("Test RequestBuilder") {
    val testInput = TestInput(b = 100, d = "Tue, 21 Feb 2017 09:32:34 GMT")
    val config = QSConfig()
    val operation = Operation(
      config = config,
      apiName = "Test API",
      method = "GET",
      requestUri = "/abc/xyz",
      statusCodes = Array.emptyIntArray
    )
    val requestBuilder = RequestBuilder(operation, testInput)
    val request = requestBuilder.build
    assert(requestBuilder.parsedHeaders == Map[String, String]("Date" -> "Tue, 21 Feb 2017 09:32:34 GMT"))
    assert(requestBuilder.parsedParams == Map[String, Any]("bar" -> "100"))
    assert(requestBuilder.parsedBody == HttpEntity.Empty)
    assert(request.method.equals(HttpMethods.GET))
    assert(request.uri.toString == "https://qingstor.com:443/abc/xyz?bar=100")
  }

  test("Test RequestBuilder with Chinese") {
    val testInput = TestInput(b = 100, d = "Tue, 21 Feb 2017 09:32:34 GMT")
    val config = QSConfig()
    val operation = Operation(
      config = config,
      apiName = "Test API",
      method = "GET",
      requestUri = s"/${QSConstants.BucketNamePlaceHolder}/${QSConstants.ObjectKeyPlaceHolder}",
      statusCodes = Array.emptyIntArray,
      bucketName = "mybucket",
      objectKey = "中文"
    )
    val requestBuilder = RequestBuilder(operation, testInput)
    val request = requestBuilder.build
    assert(request.uri.toString() == "https://qingstor.com:443/mybucket/%E4%B8%AD%E6%96%87?bar=100")
  }

  case class TestInputWithBody(date: String, body: File) extends Input {
    @ParamAnnotation(location = QSConstants.ParamsLocationHeader, name = "Date")
    def getDate: String = date

    @ParamAnnotation(location = QSConstants.ParamsLocationBody, name = "Body")
    def getBody: File = body
  }

  test("Test RequestBuilder with Body") {
    val file = new File("LICENSE")
    val testInputWithBody = TestInputWithBody(date = "Tue, 21 Feb 2017 09:32:34 GMT", body = file)
    val config = QSConfig()
    val operation = Operation(
      config = config,
      apiName = "Test API",
      method = "PUT",
      requestUri = "/",
      statusCodes = Array.emptyIntArray
    )
    val requestBuilder = RequestBuilder(operation, testInputWithBody)
    val request = requestBuilder.build
    val length = file.length()
    assert(requestBuilder.parsedHeaders == Map("Date" -> "Tue, 21 Feb 2017 09:32:34 GMT"))
    assert(request.entity.contentLengthOption.contains(length))
    assert(requestBuilder.parsedParams == Map.empty)
    assert(request.method.equals(HttpMethods.PUT))
    assert(request.uri.toString() == "https://qingstor.com:443/")
  }
}
