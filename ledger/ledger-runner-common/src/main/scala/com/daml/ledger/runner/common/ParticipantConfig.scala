// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.ledger.runner.common

import com.daml.lf.data.Ref
import com.daml.platform.configuration.IndexConfiguration
import com.daml.platform.indexer.IndexerConfig
import com.daml.ports.Port

import java.nio.file.Path
import java.time.Duration

final case class ParticipantConfig(
    mode: ParticipantRunMode,
    participantId: Ref.ParticipantId,
    // A name of the participant shard in a horizontally scaled participant.
    shardName: Option[String],
    address: Option[String],
    port: Port,
    portFile: Option[Path],
    serverJdbcUrl: String,
    managementServiceTimeout: Duration = CliParticipantConfig.DefaultManagementServiceTimeout,
    indexerConfig: IndexerConfig,
    indexConfiguration: IndexConfiguration,
    apiServerDatabaseConnectionPoolSize: Int =
      CliParticipantConfig.DefaultApiServerDatabaseConnectionPoolSize,
    apiServerDatabaseConnectionTimeout: Duration =
      CliParticipantConfig.DefaultApiServerDatabaseConnectionTimeout,
) {
  def metricsRegistryName: String = participantId + shardName.map("-" + _).getOrElse("")
}
