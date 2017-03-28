package com.qingstor.sdk.steps

import java.util.NoSuchElementException

import com.qingstor.sdk.config.QSConfig
import com.qingstor.sdk.service.Bucket
import com.qingstor.sdk.service.Types.CORSRuleModel
import cucumber.api.java8.StepdefBody._
import cucumber.api.java8.En
import com.qingstor.sdk.steps.TestUtil.TestConfig
import spray.json._
import com.qingstor.sdk.service.QSJsonProtocol.cORSRuleModelFormat

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class BucketCORSSteps extends En{

  private def initBucket(): Unit = {
    BucketCORSSteps.config = TestUtil.getQSConfig
    BucketCORSSteps.testConfig = TestUtil.getTestConfig
    BucketCORSSteps.bucket = Bucket(
      BucketCORSSteps.config,
      BucketCORSSteps.testConfig.bucket_name,
      BucketCORSSteps.testConfig.zone
    )
  }

  When("^put bucket CORS:$", new A1[String] {
    override def accept(arg: String): Unit = {
      initBucket()
      val json = arg.parseJson
      val rules = json.asJsObject.fields("cors_rules").asInstanceOf[JsArray].elements
        .toList.map(_.convertTo[CORSRuleModel])

      val input = Bucket.PutBucketCORSInput(rules)
      val outputFuture = BucketCORSSteps.bucket.putBucketCORS(input)
      BucketCORSSteps.putBucketCORSOutput = Await.result(outputFuture, Duration.Inf)
    }
  })

  Then("^put bucket CORS status code is (\\d+)$", new A1[Integer] {
    override def accept(arg: Integer): Unit = {
      val status = BucketCORSSteps.putBucketCORSOutput.statusCode.getOrElse(-1)
      assert(status == arg) 
    }
  })

  When("^get bucket CORS$", new A0 {
    override def accept(): Unit = {
      val input = Bucket.GetBucketCORSInput()
      val outputFuture = BucketCORSSteps.bucket.getBucketCORS(input)
      BucketCORSSteps.getBucketCORSOutput = Await.result(outputFuture, Duration.Inf)
    }
  })

  Then("^get bucket CORS status code is (\\d+)$", new A1[Integer]{
    override def accept(arg: Integer): Unit = {
      val status = BucketCORSSteps.getBucketCORSOutput.statusCode.getOrElse(-1)
      assert(status == arg)
    }
  })

  And("get bucket CORS should have allowed origin \"(.*)\"$", new A1[String] {
    override def accept(arg: String): Unit = {
      val hasAO = BucketCORSSteps.getBucketCORSOutput.`cors_rules`
        .map(_.exists(_.`allowed_origin` == arg))
      if (hasAO.isEmpty || !hasAO.get)
        throw new NoSuchElementException("""Allowed origin: "%s" not found in bucket CORS""".format(arg))
    }
  })

  When("^delete bucket CORS$", new A0 {
    override def accept(): Unit = {
      val input = Bucket.DeleteBucketCORSInput()
      val outputFuture = BucketCORSSteps.bucket.deleteBucketCORS(input)
      BucketCORSSteps.deleteBucketCORSOutput = Await.result(outputFuture, Duration.Inf)
    }
  })

  Then("^delete bucket CORS status code is (\\d+)$", new A1[Integer] {
    override def accept(arg: Integer): Unit = {
      val status = BucketCORSSteps.deleteBucketCORSOutput.statusCode.getOrElse(-1)
      assert(status == arg)
    }
  })
}

object BucketCORSSteps {
  private var config: QSConfig = _
  private var testConfig: TestConfig = _
  private var bucket: Bucket = _

  private var putBucketCORSOutput: Bucket.PutBucketCORSOutput = _
  private var getBucketCORSOutput: Bucket.GetBucketCORSOutput = _
  private var deleteBucketCORSOutput: Bucket.DeleteBucketCORSOutput = _
}
