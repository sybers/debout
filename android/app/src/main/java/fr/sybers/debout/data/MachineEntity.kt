package fr.sybers.debout.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.sybers.debout.domain.MacAddress
import fr.sybers.debout.domain.MacAddressParseResult
import fr.sybers.debout.domain.Machine
import fr.sybers.debout.domain.MachineType

@Entity(tableName = "machines")
data class MachineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: MachineType,
    val mac: String,
    val ip: String?,
    val hostname: String?,
    val broadcast: String?,
    @ColumnInfo(name = "wol_port") val wolPort: Int,
    @ColumnInfo(name = "probe_port") val probePort: Int?,
    @ColumnInfo(name = "sort_order") val order: Int,
)

fun MachineEntity.toDomain() = Machine(
    id = id,
    name = name,
    type = type,
    mac = (MacAddress.parse(mac) as MacAddressParseResult.Valid).mac,
    ip = ip,
    hostname = hostname,
    broadcast = broadcast,
    wolPort = wolPort,
    probePort = probePort,
    order = order,
)

fun Machine.toEntity() = MachineEntity(
    id = id,
    name = name,
    type = type,
    mac = mac.toString(),
    ip = ip,
    hostname = hostname,
    broadcast = broadcast,
    wolPort = wolPort,
    probePort = probePort,
    order = order,
)
