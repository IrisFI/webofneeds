package won.protocol.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import won.protocol.message.WonMessageType;
import won.protocol.model.MessageEventPlaceholder;

import javax.persistence.LockModeType;
import java.net.URI;
import java.util.Date;
import java.util.List;

public interface MessageEventRepository extends WonRepository<MessageEventPlaceholder> {

    MessageEventPlaceholder findOneByMessageURI(URI URI);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select msg from MessageEventPlaceholder msg where msg.messageURI = :uri")
    MessageEventPlaceholder findOneByMessageURIforUpdate(@Param("uri") URI uri);

    List<MessageEventPlaceholder> findByParentURI(URI URI);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.messageType = :messageType")
    List<MessageEventPlaceholder> findByParentURIAndMessageTypeForUpdate(
            @Param("parent") URI parentURI,
            @Param("messageType") WonMessageType messageType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and " +
            "referencedByOtherMessage = false")
    List<MessageEventPlaceholder> findByParentURIAndNotReferencedByOtherMessageForUpdate(
            @Param("parent") URI parentURI);

    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent")
    Slice<MessageEventPlaceholder> findByParentURI(@Param("parent") URI parentURI, Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent")
    Slice<MessageEventPlaceholder> findByParentURIFetchDatasetEagerly(@Param("parent") URI parentURI, Pageable pageable);

    @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.messageType = :messageType")
    Slice<URI> getMessageURIsByParentURI(
            @Param("parent") URI parentURI,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.messageType = :messageType")
    Slice<MessageEventPlaceholder> findByParentURIAndType(
            @Param("parent") URI parentURI,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.messageType = :messageType")
    Slice<MessageEventPlaceholder> findByParentURIAndTypeFetchDatasetEagerly(
            @Param("parent") URI parentURI,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate < :referenceDate")
    Slice<URI> getMessageURIsByParentURIBefore(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate < :referenceDate")
    Slice<MessageEventPlaceholder> findByParentURIBefore(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate < :referenceDate")
    Slice<MessageEventPlaceholder> findByParentURIBeforeFetchDatasetEagerly(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate < (select msg2.creationDate from MessageEventPlaceholder msg2 where msg2.messageURI = :referenceMessageUri )")
    Slice<MessageEventPlaceholder> findByParentURIBeforeFetchDatasetEagerly(
            @Param("parent") URI parentURI,
            @Param("referenceMessageUri") URI referenceMessageUri,
            Pageable pageable);


    @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate < :referenceDate and msg.messageType = :messageType")
    Slice<URI> getMessageURIsByParentURIBefore(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate < :referenceDate and msg.messageType = :messageType")
    Slice<MessageEventPlaceholder> findByParentURIAndTypeBefore(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate < :referenceDate and msg.messageType = :messageType")
    Slice<MessageEventPlaceholder> findByParentURIAndTypeBeforeFetchDatasetEagerly(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.messageType = :messageType and msg.creationDate < (select msg2.creationDate from MessageEventPlaceholder msg2 where msg2.messageURI = :referenceMessageUri )")
    Slice<MessageEventPlaceholder> findByParentURIAndTypeBeforeFetchDatasetEagerly(
            @Param("parent") URI parentURI,
            @Param("referenceMessageUri") URI referenceMessageURI,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate > :referenceDate")
    Slice<URI> getMessageURIsByParentURIAfter(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate > " +
            ":referenceDate")
    Slice<MessageEventPlaceholder> findByParentURIAfter(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate > " +
            ":referenceDate")
    Slice<MessageEventPlaceholder> findByParentURIAfterFetchDatasetEagerly(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            Pageable pageable);

    @Query("select messageURI from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate > :referenceDate and msg.messageType = :messageType")
    Slice<URI> getMessageURIsByParentURIAfter(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);


    @Query("select msg from MessageEventPlaceholder msg left join fetch msg.datasetHolder where msg.parentURI = :parent and msg.creationDate > :referenceDate and msg.messageType = :messageType")
    Slice<MessageEventPlaceholder> findByParentURIAndTypeAfter(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);

    @Query("select msg from MessageEventPlaceholder msg where msg.parentURI = :parent and msg.creationDate > :referenceDate and msg.messageType = :messageType")
    Slice<MessageEventPlaceholder> findByParentURIAndTypeAfterFetchDatasetEagerly(
            @Param("parent") URI parentURI,
            @Param("referenceDate") Date referenceDate,
            @Param("messageType") WonMessageType messageType,
            Pageable pageable);



    @Query("select msg from MessageEventPlaceholder msg where msg.correspondingRemoteMessageURI = :uri")
    MessageEventPlaceholder findOneByCorrespondingRemoteMessageURI(@Param("uri") URI uri);



    @Query("select max(msg.creationDate) from MessageEventPlaceholder msg where msg.creationDate <= :referenceDate and " +
            "parentURI = :parent")
    Date findMaxActivityDateOfParentAtTime(@Param("parent") URI parentURI, @Param("referenceDate") Date referenceDate);

    @Query("select max(msg.creationDate) from MessageEventPlaceholder msg where msg.creationDate <= :referenceDate and " +
            "parentURI = :parent and msg.messageType = :messageType")
    Date findMaxActivityDateOfParentAtTime(@Param("parent") URI parentURI, @Param("messageType") WonMessageType
            messageType, @Param("referenceDate") Date referenceDate);

}
