/*
 * Copyright (C) 2020 ActiveJ LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.activej.cube.ot;

import io.activej.aggregation.Aggregation;
import io.activej.aggregation.ot.AggregationDiff;
import io.activej.aggregation.ot.AggregationDiffCodec;
import io.activej.codec.StructuredCodec;
import io.activej.codec.StructuredInput;
import io.activej.codec.StructuredOutput;
import io.activej.common.parse.ParseException;
import io.activej.cube.Cube;

import java.util.LinkedHashMap;
import java.util.Map;

public class CubeDiffCodec implements StructuredCodec<CubeDiff> {
	private final Map<String, AggregationDiffCodec> aggregationDiffCodecs;

	private CubeDiffCodec(Map<String, AggregationDiffCodec> aggregationDiffCodecs) {
		this.aggregationDiffCodecs = aggregationDiffCodecs;
	}

	public static CubeDiffCodec create(Cube cube) {
		Map<String, AggregationDiffCodec> aggregationDiffCodecs = new LinkedHashMap<>();

		for (String aggregationId : cube.getAggregationIds()) {
			Aggregation aggregation = cube.getAggregation(aggregationId);
			AggregationDiffCodec aggregationDiffCodec = AggregationDiffCodec.create(aggregation.getStructure());
			aggregationDiffCodecs.put(aggregationId, aggregationDiffCodec);
		}
		return new CubeDiffCodec(aggregationDiffCodecs);
	}

	@Override
	public void encode(StructuredOutput out, CubeDiff cubeDiff) {
		out.writeObject(() -> {
			for (String aggregation : aggregationDiffCodecs.keySet()) {
				AggregationDiff aggregationDiff = cubeDiff.get(aggregation);
				if (aggregationDiff == null)
					continue;
				AggregationDiffCodec aggregationDiffCodec = aggregationDiffCodecs.get(aggregation);
				out.writeKey(aggregation);
				aggregationDiffCodec.encode(out, aggregationDiff);
			}
		});
	}

	@Override
	public CubeDiff decode(StructuredInput in) throws ParseException {
		return in.readObject($ -> {
			Map<String, AggregationDiff> map = new LinkedHashMap<>();
			while (in.hasNext()) {
				String aggregation = in.readKey();
				AggregationDiffCodec aggregationDiffCodec = aggregationDiffCodecs.get(aggregation);
				if (aggregationDiffCodec == null) {
					throw new ParseException("Unknown aggregation: " + aggregation);
				}
				AggregationDiff aggregationDiff = aggregationDiffCodec.decode(in);
				map.put(aggregation, aggregationDiff);
			}

			return CubeDiff.of(map);
		});
	}

}