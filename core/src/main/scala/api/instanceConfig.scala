/** dumping ground for some stuff **/


//case class InstanceConfig(managedDevices: Seq[ManagedDevice]) {
//  def devices = managedDevices.map(_.device).toList.sequenceU
//  def requests = {
//    managedDevices.map { md =>
//      md.device.disjunction.flatMap { d => md.requests(d).disjunction }
//    }.toList.sequenceU.map(_.flatten).validation
//  }
//}
//
//trait ManagedDevice {
//  type Request <: RequestLike
//  type ScheduledRequest = (Request, Schedule)
//  type Device = Request#Device
//  def device: ValidationNel[ConfigError, Device]
//  def requests(d: Device): ValidationNel[ConfigError, Seq[ScheduledRequest]]
//}
//
//trait ConfigError
//case class NonUniqueDeviceIds(repeated: Set[DeviceId]) extends ConfigError
//case class DeviceConfigError(msg: String) extends ConfigError

//private[this] def validateConfig(c: InstanceConfig) = {
//  (checkForDupes(c) >>
//    ((c.devices |@| c.requests).tupled).disjunction
//  ).validation
//}
//
//private[this] def checkForDupes(c: InstanceConfig) = {
//  c.devices.disjunction.flatMap { ds =>
//    noDuplicates(ds.map(_.id)).bimap(
//      dupes => NonEmptyList.nels(NonUniqueDeviceIds(dupes)),
//      ids => c
//    )
//  }
//}
//
//private[this] def noDuplicates[T](ts: Seq[T]): \/[Set[T], Seq[T]] = {
//  val distinct = ts.distinct
//  if (ts.length == distinct.length) ts.right else ts.diff(distinct).toSet.left
//}
//
