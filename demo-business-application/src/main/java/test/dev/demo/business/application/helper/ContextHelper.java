package test.dev.demo.business.application.helper;

import lombok.experimental.UtilityClass;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import test.dev.demo.business.application.dto.ConsumerContextDto;
import test.dev.demo.business.application.dto.ProducerContextDto;
import test.dev.demo.business.application.dto.SameMessageProducerContextDto;
import test.dev.demo.business.application.dto.SmokeContextDto;
import test.dev.demo.business.application.kafka.ConsumerContext;
import test.dev.demo.business.application.kafka.ProducerContext;
import test.dev.demo.business.application.kafka.SmokeContext;
import test.dev.smartreplication.core.compress.CompressResult;
import test.dev.smartreplication.core.compress.IChangeCompressor;
import test.dev.smartreplication.model.Change;
import test.dev.smartreplication.model.ChangeItem;
import test.dev.smartreplication.model.ChangeOperation;
import test.dev.smartreplication.model.ChangeSource;
import test.dev.smartreplication.model.TableId;

import java.util.function.BiFunction;

@UtilityClass
public class ContextHelper {
    public static final String ERROR_PREFIX = "error";

    private static final BiFunction<Integer, ProducerContext<Change>, Change> CHANGE_GENERATOR =
        (id, context) -> Change.newBuilder()
            .setOwner(context.getMeta().getOwner())
            .setChangeSource(ChangeSource.STAND_IN)
            .setBusinessOperationId("BusinessOperationId" + id)
            .setChangeKey("changeKey " + id)
            .setChangeType(context.getMeta().getType())
            .setChangeVersionId("changeVersionId" + id)
            .setTransactionId((long) id)
            .addChangeItem(ChangeItem.newBuilder(ChangeOperation.INSERT, TableId.of("table-1"), 1))
            .build();

    private static final BiFunction<Integer, ProducerContext<Change>, Change> ERROR_GENERATOR =
        (id, context) -> Change.newBuilder()
            .setOwner(context.getMeta().getOwner())
            .setChangeSource(ChangeSource.STAND_IN)
            .setBusinessOperationId("BusinessOperationId" + id)
            .setChangeKey(ERROR_PREFIX + "changeKey " + id)
            .setChangeType(context.getMeta().getType())
            .setChangeVersionId("changeVersionId" + id)
            .setTransactionId((long) id)
            .addChangeItem(ChangeItem.newBuilder(ChangeOperation.INSERT, TableId.of("table-1"), 1))
            .build();

    public static <R> ProducerContext<R> prepareProducerContext(
        final ProducerContextDto ctx,
        final BiFunction<Integer, ProducerContext<R>, R> creator
    ) {
        return ProducerContext.<R>builder()
            .keySize(ctx.getKeySize())
            .size(ctx.getSize())
            .numThreads(ctx.getThread())
            .meta(ctx.getMeta())
            .creator(creator)
            .build();
    }

    public static ProducerContext<Change> forProducer(ProducerContextDto ctx) {
        return ContextHelper.prepareProducerContext(ctx, CHANGE_GENERATOR);
    }

    public static SmokeContext<Change> forSmoke(SmokeContextDto ctx) {
        final var producerContext = ctx.getProducerContext();
        producerContext.setMeta(ctx.getMeta());

        final var consumerContext = ctx.getConsumerContext();
        consumerContext.setMeta(ctx.getMeta());

        return SmokeContext.<Change>builder()
            .producerContext(ContextHelper.forProducer(producerContext))
            .consumerContext(ContextHelper.forConsumer(consumerContext))
            .build();
    }

    public static ProducerContext<Change> error(ProducerContextDto ctx) {
        return ContextHelper.prepareProducerContext(ctx, ERROR_GENERATOR);
    }

    public static ProducerContext<WrongMessage> fake(ProducerContextDto contextDto) {
        return ContextHelper.prepareProducerContext(
            contextDto,
            (id, ctx) -> WrongMessage.builder().text("Ops!").build());
    }

    public static ConsumerContext forConsumer(ConsumerContextDto ctx) {
        return ConsumerContext.builder()
            .meta(ctx.getMeta())
            .concurrency(ctx.getConcurrency())
            .maxAttempts(ctx.getMaxAttempts())
            .readLimit(ctx.getReadLimit())
            .build();
    }

    public static ProducerContext<Change> manual(SameMessageProducerContextDto ctx) {
        return ProducerContext.<Change>builder()
            .key(ctx.getKey())
            .size(ctx.getSize())
            .numThreads(1)
            .meta(ctx.getMeta())
            .creator((i, context) -> ctx.getChange())
            .build();
    }

    public static ProducerContext<byte[]> compress(SameMessageProducerContextDto ctx, IChangeCompressor changeCompression) {
        return ProducerContext.<byte[]>builder()
            .key(ctx.getKey())
            .size(ctx.getSize())
            .numThreads(1)
            .meta(ctx.getMeta())
            .creator((i, context) -> {
                CompressResult wrapped = changeCompression.compress(ctx.getChange());
                return wrapped.asByteArray();
            })
            .valueSerializer(ByteArraySerializer.class)
            .build();
    }
}
