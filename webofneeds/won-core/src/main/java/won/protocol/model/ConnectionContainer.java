/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.model;

import won.protocol.model.parentaware.ParentAware;
import won.protocol.model.parentaware.VersionedEntity;

import javax.persistence.*;
import java.util.Collection;
import java.util.Date;

@Entity
@Table(name="connection_container")
public class ConnectionContainer implements ParentAware<Need>, VersionedEntity
{
  @Id
  @Column( name = "id" )
  protected Long id;

  @OneToMany (fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private Collection<Connection> connections;

  @Column(name="version", columnDefinition = "integer DEFAULT 0", nullable = false)
  private int version = 0;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name="last_update", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
  private Date lastUpdate = new Date();

  @OneToOne (fetch = FetchType.LAZY, optional = false)
  @MapsId
  private Need need;

  public Collection<Connection> getConnections() {
    return connections;
  }

  @Override
  @PrePersist
  @PreUpdate
  public void incrementVersion() {
    this.version++;
    this.lastUpdate = new Date();
  }

  @Override
  public Date getLastUpdate() {
    return lastUpdate;
  }

  public int getVersion() {
    return version;
  }

  public Need getNeed() {
    return need;
  }

  @Override
  public Need getParent() {
    return getNeed();
  }

  public void setNeed(final Need need) {
    this.need = need;
  }

  public ConnectionContainer(final Need need) {
    this.need = need;
    if (need != null) {
      need.setConnectionContainer(this);
    }
  }

  public ConnectionContainer() {
  }
}
