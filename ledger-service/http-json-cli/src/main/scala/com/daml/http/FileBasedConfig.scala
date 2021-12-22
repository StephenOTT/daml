// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.http

import akka.stream.ThrottleMode
import com.daml.cliopts
import com.daml.cliopts.Logging.LogEncoder
import com.daml.http.dbbackend.{DbStartupMode, JdbcConfig}
import com.daml.metrics.MetricsReporter
import com.daml.pureconfigutils.{HttpServerConfig, LedgerApiConfig, MetricsConfig}
import com.daml.pureconfigutils.SharedConfigReaders._
import pureconfig.{ConfigReader, ConvertHelpers}
import pureconfig.generic.semiauto._
import ch.qos.logback.classic.{Level => LogLevel}

import scala.concurrent.duration._

private[http] object FileBasedConfig {
  implicit val timeProviderTypeCfgReader: ConfigReader[ThrottleMode] =
    ConfigReader.fromString[ThrottleMode](ConvertHelpers.catchReadError { s =>
      s.toLowerCase() match {
        case "enforcing" => ThrottleMode.Enforcing
        case "shaping" => ThrottleMode.Shaping
        case s =>
          throw new IllegalArgumentException(
            s"Value '$s' for throttle-mode is not one of 'shaping' or 'enforcing'"
          )
      }
    })
  implicit val websocketCfgReader: ConfigReader[WebsocketConfig] =
    deriveReader[WebsocketConfig]
  implicit val staticContentCfgReader: ConfigReader[StaticContentConfig] =
    deriveReader[StaticContentConfig]

  implicit val dbStartupModeReader: ConfigReader[DbStartupMode] =
    ConfigReader.fromString[DbStartupMode](ConvertHelpers.catchReadError { s =>
      DbStartupMode.configValuesMap.getOrElse(
        s.toLowerCase(),
        throw new IllegalArgumentException(
          s"Value $s for db-startup is not one of ${DbStartupMode.allConfigValues.mkString(",")}"
        ),
      )
    })
  implicit val queryStoreCfgReader: ConfigReader[JdbcConfig] = deriveReader[JdbcConfig]

  implicit val httpJsonApiCfgReader: ConfigReader[FileBasedConfig] =
    deriveReader[FileBasedConfig]

  val Empty: FileBasedConfig = FileBasedConfig(
    HttpServerConfig(cliopts.Http.defaultAddress, -1),
    LedgerApiConfig("", -1),
  )
}
private[http] final case class FileBasedConfig(
    server: HttpServerConfig,
    ledgerApi: LedgerApiConfig,
    queryStore: Option[JdbcConfig] = None,
    packageReloadInterval: FiniteDuration = StartSettings.DefaultPackageReloadInterval,
    maxInboundMessageSize: Int = StartSettings.DefaultMaxInboundMessageSize,
    healthTimeoutSeconds: Int = StartSettings.DefaultHealthTimeoutSeconds,
    packageMaxInboundMessageSize: Option[Int] = None,
    maxTemplateIdCacheEntries: Option[Long] = None,
    websocketConfig: Option[WebsocketConfig] = None,
    metrics: Option[MetricsConfig] = None,
    allowInsecureTokens: Boolean = false,
    staticContent: Option[StaticContentConfig] = None,
) {
  def toConfig(
      nonRepudiation: nonrepudiation.Configuration.Cli,
      logLevel: Option[LogLevel], // the default is in logback.xml
      logEncoder: LogEncoder,
      metricsReporter: Option[MetricsReporter],
      metricsReportingInterval: FiniteDuration,
  ): Config = {
    Config(
      ledgerApi.address,
      ledgerApi.port,
      server.address,
      server.port,
      server.portFile,
      packageReloadInterval,
      packageMaxInboundMessageSize,
      maxInboundMessageSize,
      healthTimeoutSeconds,
      ledgerApi.tls.tlsConfiguration,
      queryStore,
      staticContent,
      allowInsecureTokens,
      websocketConfig,
      nonRepudiation,
      logLevel,
      logEncoder,
      metricsReporter,
      metrics.map(_.reportingInterval).getOrElse(metricsReportingInterval),
      maxTemplateIdCacheEntries,
    )
  }
}
