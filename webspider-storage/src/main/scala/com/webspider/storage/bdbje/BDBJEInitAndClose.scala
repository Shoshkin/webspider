package com.webspider.storage.bdbje

import com.webspider.storage.MustInitAndClose
import java.io.File
import com.sleepycat.je._
import java.util
import com.sleepycat.bind.tuple.TupleBinding
import persist.LinkSerializer
import com.webspider.core.{Link, LinkStorageState}

/**
 * User: Eugene Dzhurinsky
 * Date: 2/15/13
 */
trait BDBJEInitAndClose extends MustInitAndClose[Environment] {

  val dbPath: File

  protected var cfg: EnvironmentConfig = _

  protected var env: Environment = _

  protected var mainDatabase: Database = _

  protected var urlDatabase: Database = _

  protected var relationDatabase: Database = _

  protected var queueDatabase: SecondaryDatabase = _

  protected var inprogressDatabase: SecondaryDatabase = _

  val linkSerializer = LinkSerializer.linkSerializer

  override def init() {
    dbPath.mkdirs()
    cfg = new EnvironmentConfig().setAllowCreate(true).setTransactional(true)
    env = new Environment(dbPath, cfg)
    def dbCfg(f: (DatabaseConfig) => DatabaseConfig = identity) = {
      f(new DatabaseConfig().setAllowCreate(true).setTransactional(true))
    }
    mainDatabase = env.openDatabase(null, "mainDb", dbCfg())
    urlDatabase = env.openDatabase(null, "urlDb", dbCfg())
    relationDatabase = env.openDatabase(null, "relationDb", dbCfg {
      case dbCfgLocal => dbCfgLocal.setSortedDuplicates(true)
    })
    def secondaryCfg(state: LinkStorageState.Value) = {
      val cfg = new SecondaryConfig().
        setAllowPopulate(true).
        setMultiKeyCreator(new SecondaryMultiKeyCreator {
        def createSecondaryKeys(secondary: SecondaryDatabase,
                                key: DatabaseEntry,
                                data: DatabaseEntry,
                                results: util.Set[DatabaseEntry]) {
          val linkObject: Link = linkSerializer.entryToObject(data)
          if (linkObject.storageState == state) {
            val dbe = new DatabaseEntry()
            TupleBinding.getPrimitiveBinding(classOf[Long]).objectToEntry(linkObject.queuedAt, dbe)
            results.add(dbe)
          }
        }
      }
      )
      cfg.setAllowCreate(true)
      cfg.setTransactional(true)
      cfg
    }
    queueDatabase = env.openSecondaryDatabase(null, "queueDb", mainDatabase, secondaryCfg(LinkStorageState.QUEUED))
    inprogressDatabase = env.openSecondaryDatabase(null, "inProgress", mainDatabase, secondaryCfg(LinkStorageState.IN_PROGRESS))
  }

  override def close(f: Environment => Unit) {
    List(queueDatabase, relationDatabase, inprogressDatabase, mainDatabase, urlDatabase).filter(_ != null).foreach {
      closable => try {
        closable.close()
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
    f(env)
    env.close()
  }

}